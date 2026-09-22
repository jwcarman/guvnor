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

import java.util.stream.Collectors;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.api.Harness;
import org.springframework.stereotype.Component;

/**
 * Hands the agent what the quarantine extracted, and never the email.
 *
 * <p>This is the half of lesson 4 that lesson 3 could not do. Compare what gets built here against
 * lessons 1 to 3, where the customer's words were concatenated into the prompt and the model was
 * asked to be sensible about them.
 *
 * <p>The privileged agent -- the one holding tools that move money -- is given two fields: a kind,
 * which is an enum with four possible answers, and an amount, which has already been parsed into
 * {@code Money} or rejected. There is nowhere in that for a sentence to hide, so there is nothing
 * for an instruction to arrive in.
 *
 * <p>The quarantined model read the email and may well have been fooled by it. That is allowed. It
 * had no tools.
 */
@Component
public class DeskAgent {

  private final Harness<String> harness;
  private final GuardedMessages messages;
  private final ChargeService charges;

  private final Derivation<String, String> redacted;
  private final Quarantine quarantine;

  public DeskAgent(
      Harness<String> harness,
      GuardedMessages messages,
      ChargeService charges,
      Derivation<String, String> redacted,
      Quarantine quarantine) {
    this.harness = harness;
    this.messages = messages;
    this.charges = charges;

    this.redacted = redacted;
    this.quarantine = quarantine;
  }

  public void handle(MessageId id) {
    Message message = messages.find(id).orElseThrow();

    // Already concealed, at the edge, before this class existed in the story.
    Surrogate<String> mail = messages.concealed(id);

    // Lesson 3's protection still applies, and applies to the quarantined model too: it is a
    // model, so it is a third party that keeps what it is shown, so it does not get the card.
    // The two protections compose because they are answers to different questions.
    Surrogate<String> safe = redacted.derive(mail).orThrow();

    // Read behind glass, by a model with nothing to act with. What comes back is already a
    // governed value: shaped, and untrusted.
    Surrogate<Claim> claim = quarantine.read(safe, message.from());

    String context =
        charges.forAccount(message.from()).stream()
            .map(Charge::id)
            .map(chargeId -> "charge " + chargeId)
            .collect(Collectors.joining(", "));

    // Two fields and an account. Note what is absent: every word the customer wrote.
    harness.observe(
        Desk.forMessage(id),
        """
        A customer has written in. They are account %s. On their account: %s.

        There is a claim from them: %s. Act on it, or do not."""
            .formatted(message.from(), context.isEmpty() ? "no charges" : context, claim.id()));
  }
}
