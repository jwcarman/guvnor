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
package org.jwcarman.guvnor.concealed;

import java.util.stream.Collectors;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Revealed;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.api.Harness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Hands an email to the agent, without ever holding it.
 *
 * <p>The body is read once, at the edge, and immediately concealed. From that line on it is a
 * {@link Surrogate} -- a reference that can be passed around, logged, stored and compared, and that
 * cannot be read without naming where it is going.
 *
 * <p>Nothing here decides what the model may see. The charter does, and it refuses.
 */
@Component
public class DeskAgent {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  private final Harness<String> harness;
  private final MessageService messages;
  private final ChargeService charges;
  private final Conceal<String> inbound;
  private final Derivation<String, String> redacted;
  private final Reveal<String> model;

  public DeskAgent(
      Harness<String> harness,
      MessageService messages,
      ChargeService charges,
      Conceal<String> inbound,
      Derivation<String, String> redacted,
      Reveal<String> model) {
    this.harness = harness;
    this.messages = messages;
    this.charges = charges;
    this.inbound = inbound;
    this.redacted = redacted;
    this.model = model;
  }

  public void handle(MessageId id) {
    Message message = messages.find(id).orElseThrow();

    // The last moment this application holds what the customer wrote.
    Surrogate<String> mail = inbound.conceal(messages.body(id).orElse(""));

    // What a careless version of this class would try, and what stops it. Nothing was
    // sanitised, so the mail is still labelled cardholder, and the model's door does not
    // admit cardholder data. This is not a check somebody wrote; it is the door.
    Revealed<String> straightToTheModel = model.reveal(mail);
    if (straightToTheModel instanceof Revealed.Denied<String>(var reason, var detail)) {
      LOG.info("the model may not read the mail as it arrived: {} -- {}", reason, detail);
    }

    // The one declared route from cardholder to personal, by name.
    Surrogate<String> safe = redacted.derive(mail).orThrow();
    String forTheModel = model.reveal(safe).orThrow();

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
            .formatted(message.from(), context.isEmpty() ? "no charges" : context, forTheModel));
  }
}
