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

import java.util.Optional;
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.loch.AccessContext;
import org.jwcarman.nessy.api.extraction.Extraction;
import org.jwcarman.nessy.api.extraction.Extractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The quarantined read: one model call, with nothing to act with.
 *
 * <p>A model is shown a document and a shape, and the only thing it can do is fill the shape in.
 * Whatever the email asked for, an enum and an amount cannot carry it out.
 *
 * <p>This runs as the body of a declared derivation, which is the only way it gets to see the
 * plaintext at all. It is handed the mail by the charter, not by a caller who decided it was
 * allowed -- and what it returns is a claim whose lineage records the mail it came from.
 *
 * <p>It does not stop the model being talked into recording the wrong thing, and is not trying to.
 * A document may well persuade it that the customer wants $999.00 of goodwill. What it cannot do is
 * reach a model that can act.
 */
@Component
public class Quarantine {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  private final Extractor extractor;

  public Quarantine(Extractor extractor) {
    this.extractor = extractor;
  }

  /**
   * Reads one email for what it appears to ask for.
   *
   * <p>The signature a Loch derivation wants: a value in, an {@link Optional} out. Answering empty
   * refuses the derivation, so mail this desk cannot act on produces no claim at all rather than a
   * claim to do nothing.
   */
  public Optional<Claim> read(String document, AccessContext context) {
    Extraction<Request> extraction = extractor.extract(Request.class, document);

    Request request =
        switch (extraction) {
          case Extraction.Extracted<Request>(Request read, var usage) -> {
            LOG.info("the quarantined read says: {} of {}", read.kind(), read.amount());
            yield read;
          }
          case Extraction.Refused<Request>(String category, var usage) -> {
            LOG.info("the quarantined model declined to read this: {}", category);
            yield null;
          }
          case Extraction.Talked<Request>(String said, var usage) -> {
            // It answered the document instead of filling the shape in, which is exactly the
            // failure this arrangement contains: saying something is all it can do.
            LOG.info("the quarantined model talked instead of filling the shape in");
            yield null;
          }
          case Extraction.Failed<Request>(String reason, var usage) -> {
            LOG.info("the quarantined read failed: {}", reason);
            yield null;
          }
        };

    if (request == null || request.kind() == Request.Kind.OTHER) {
      return Optional.empty();
    }
    return account(context).map(who -> Claim.unsupported(who, amountOf(request), request.kind()));
  }

  /** Whose mail this is, taken from the access rather than from anything the document said. */
  private static Optional<AccountId> account(AccessContext context) {
    return context.get("account").map(java.util.UUID::fromString).map(AccountId::new);
  }

  /** The amount, parsed. Anything that is not an amount of dollars stops here. */
  private static Money amountOf(Request request) {
    try {
      return Money.fromDollars(request.amount());
    } catch (IllegalArgumentException notAnAmount) {
      LOG.info("the quarantined read produced something that is not an amount");
      return Money.usd(0L);
    }
  }
}
