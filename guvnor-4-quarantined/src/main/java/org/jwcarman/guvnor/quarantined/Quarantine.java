/*
 * Copyright © 2026 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwcarman.guvnor.quarantined;

import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.api.extraction.Extraction;
import org.jwcarman.nessy.api.extraction.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The quarantined read.
 *
 * <p>One model call, with no tools, no memory and no history: a model is shown a document and a
 * shape, and the only thing it can do is fill the shape in. Whatever the email asked for, an enum
 * and an amount cannot carry it out.
 *
 * <p>This does not stop the model being talked into recording the wrong thing, and is not trying
 * to. A document may well persuade it that the customer wants $999.00 of goodwill. What it cannot
 * do is reach a model that can act -- because the privileged side never sees the document, only two
 * fields of a shape it asked for.
 *
 * <p>What comes back is a claim, not a fact.
 */
@Component
public class Quarantine {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  private final Extractor extractor;
  private final Reveal<String> model;
  private final Conceal<Claim> claimed;

  public Quarantine(Extractor extractor, Reveal<String> model, Conceal<Claim> claimed) {
    this.extractor = extractor;
    this.model = model;
    this.claimed = claimed;
  }

  /**
   * Reads the mail for what it appears to ask for, and conceals the answer.
   *
   * <p>What comes back is not a plain object. It is a {@link Surrogate} over an unendorsed claim:
   * validated in shape, untrusted in label. Those are two separate protections and both are needed.
   *
   * <p>The shape is why an instruction cannot ride out of here -- an enum and a parsed amount have
   * nowhere to put a sentence. The label is why the thing that did come out cannot be acted on. A
   * model that is handed this can pass it along and ask for it to be acted on; it cannot author a
   * different one, and it cannot make this one worth more than it is.
   */
  public Surrogate<Claim> read(Surrogate<String> mail, AccountId account) {
    String document = model.reveal(mail).orThrow();

    Extraction<Request> extraction = extractor.extract(Request.class, document);

    Request request =
        switch (extraction) {
          case Extraction.Extracted<Request>(Request read, var usage) -> {
            LOG.info("the quarantined read says: {} of {}", read.kind(), read.amount());
            yield read;
          }
          case Extraction.Refused<Request>(String category, var usage) -> {
            LOG.info("the quarantined model declined to read this: {}", category);
            yield new Request(Request.Kind.OTHER, "0.00");
          }
          case Extraction.Talked<Request>(String said, var usage) -> {
            // It answered the document instead of filling in the shape. Which is exactly the
            // failure
            // this arrangement exists to contain: it said something, and saying something is all it
            // can do.
            LOG.info("the quarantined model talked instead of filling the shape in");
            yield new Request(Request.Kind.OTHER, "0.00");
          }
          case Extraction.Failed<Request>(String reason, var usage) -> {
            LOG.info("the quarantined read failed: {}", reason);
            yield new Request(Request.Kind.OTHER, "0.00");
          }
        };

    // Concealed the moment it exists, so there is no window in which it is an ordinary object
    // that ordinary code could act on by mistake.
    return claimed.conceal(new Claim(account, amountOf(request), request.kind(), ""));
  }

  /** The amount, parsed. Anything that is not an amount of dollars stops here. */
  public static Money amountOf(Request request) {
    try {
      return Money.fromDollars(request.amount());
    } catch (IllegalArgumentException notAnAmount) {
      LOG.info(
          "the quarantined read produced something that is not an amount: {}",
          notAnAmount.getMessage());
      return Money.usd(0L);
    }
  }
}
