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
package org.jwcarman.guvnor.careful;

import java.util.stream.Collectors;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.nessy.api.Harness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hands an email to the agent, after cleaning it.
 *
 * <p>Two things changed since lesson 1, and both are what a careful team would do. Mail whose
 * phrasing matches a known injection is refused outright and never reaches the model. Mail that
 * passes has card numbers redacted before the prompt is built.
 *
 * <p>Everything else is identical -- including the part that matters. The desk's framing and the
 * customer's words are still concatenated into one string, so provenance is still destroyed at the
 * same line. What has been added is a filter in front of it, and a filter answers the question
 * "does this look dangerous", which is not the question. The question is "who said this", and the
 * string still cannot say.
 */
@Component
public class DeskAgent {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  private final Harness<String> harness;
  private final MessageService messages;
  private final ChargeService charges;

  public DeskAgent(Harness<String> harness, MessageService messages, ChargeService charges) {
    this.harness = harness;
    this.messages = messages;
    this.charges = charges;
  }

  /** True when the mail was refused and never shown to the model. */
  public boolean handle(MessageId id) {
    Message message = messages.find(id).orElseThrow();
    String raw = messages.body(id).orElse("");

    if (Sanitiser.looksLikeAnInjection(raw)) {
      LOG.warn(
          "refused: this email contains known injection phrasing, and was not shown to the"
              + " model");
      return false;
    }
    String body = Sanitiser.redactCards(raw);

    String context =
        charges.forAccount(message.from()).stream()
            .map(Charge::id)
            .map(chargeId -> "charge " + chargeId)
            .collect(Collectors.joining(", "));

    harness.observe(
        Desk.forMessage(id),
        """
        A customer has written in. They are account %s. On their account: %s.

        %s"""
            .formatted(message.from(), context.isEmpty() ? "no charges" : context, body));
    return true;
  }
}
