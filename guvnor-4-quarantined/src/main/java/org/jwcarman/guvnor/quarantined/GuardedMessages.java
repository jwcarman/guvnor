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

import java.util.function.Consumer;
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageReceived;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Surrogate;

/**
 * The desk's mail, with the guards up before anything is stored.
 *
 * <p>The ordinary message service keeps bodies in a map. That is right for a desk with a person at
 * it and wrong for a desk with a model in it: between an email landing and something deciding to be
 * careful with it, the plaintext is an ordinary String in ordinary storage and anything at all can
 * read it.
 *
 * <p>So the plaintext never gets stored. It is concealed on the way in, and what goes into the
 * desk's own body store is the surrogate's id -- a reference, which is what everything downstream
 * gets and all any of it needs.
 *
 * <p>Mail to a billing desk may contain a card number, and certainly contains something a stranger
 * wrote. Both are true the instant it lands, not at the point somebody remembers, which is why this
 * is an override of the intake rather than a step after it.
 */
public class GuardedMessages extends MessageService {

  private final Conceal<String> inbound;

  public GuardedMessages(Consumer<MessageReceived> announce, Conceal<String> inbound) {
    super(announce);
    this.inbound = inbound;
  }

  /** Takes custody, then stores the reference. The plaintext goes no further than this line. */
  @Override
  public Message receive(AccountId from, String subject, String body) {
    return super.receive(from, subject, inbound.conceal(body).id());
  }

  /**
   * The mail, as the only thing anything here is entitled to hold.
   *
   * <p>{@code body(id)} still answers, and what it answers with is a surrogate id. That is not a
   * trick: in this lesson the body of a message genuinely is a reference, and code that treats it
   * as prose gets a reference-shaped string rather than a customer's words.
   */
  public Surrogate<String> concealed(MessageId id) {
    return body(id).<Surrogate<String>>map(Surrogate::of).orElseThrow();
  }
}
