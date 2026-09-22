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
package org.jwcarman.guvnor.naive;

import java.util.stream.Collectors;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.nessy.api.Harness;
import org.springframework.stereotype.Component;

/**
 * Hands an email to the agent.
 *
 * <p>Note what {@link #handle} builds: the account, the charges, and the customer's own words,
 * concatenated into one string. That is the obvious way to give a model the context it needs, and
 * it is how almost everyone writes this the first time.
 *
 * <p>It is also the bug. After this method returns, nothing downstream can tell which part of that
 * string the desk wrote and which part arrived from outside, because the string does not record the
 * difference. Every defence in the next few lessons is, in one way or another, about not doing
 * this.
 */
@Component
public class DeskAgent {

  private final Harness<String> harness;
  private final MessageService messages;
  private final ChargeService charges;

  public DeskAgent(Harness<String> harness, MessageService messages, ChargeService charges) {
    this.harness = harness;
    this.messages = messages;
    this.charges = charges;
  }

  public void handle(MessageId id) {
    Message message = messages.find(id).orElseThrow();
    String body = messages.body(id).orElse("");

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
  }
}
