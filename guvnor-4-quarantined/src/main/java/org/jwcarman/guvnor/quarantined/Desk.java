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

import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.nessy.api.AgentId;
import org.jwcarman.nessy.api.AgentType;

/** The one agent this application runs: a support desk that decides what to do about a claim. */
public final class Desk {

  public static final AgentType TYPE = new AgentType("dispute-desk");

  /**
   * One agent per email, not one agent for the desk.
   *
   * <p>A single long-lived agent would carry every case it has ever seen into every case it sees
   * next, so one customer's claim would be sitting in the context while another customer's is being
   * handled. A case is a conversation; the next case is a different conversation.
   *
   * <p>Derived from the message rather than random, so the conversation that handled a given email
   * can still be found afterwards. That matters in lesson 6, when somebody wants to audit what was
   * decided and why.
   */
  public static AgentId forMessage(MessageId message) {
    return new AgentId(message.value());
  }

  /**
   * What the desk is told, and what it is not shown.
   *
   * <p>There is no warning here about untrusted input, and that is the point. Lesson 2's warning
   * was an attempt to make a model careful about words it had no way to attribute. This agent is
   * never given the words.
   */
  public static final String SYSTEM_PROMPT =
      """
      You work a billing support desk. You will be told what a customer appears to be asking \
      for, as a kind and an amount. You will not be shown their message.

      Decide what to do about it. You can refund a charge with the refund tool, and you can \
      issue a goodwill credit with the issue_credit tool. Amounts are in dollars, written like \
      42.00.""";

  private Desk() {}
}
