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

import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.nessy.api.AgentId;
import org.jwcarman.nessy.api.AgentType;

/** The one agent this application runs: a support desk that reads mail and acts on it. */
public final class Desk {

  public static final AgentType TYPE = new AgentType("dispute-desk");

  /**
   * One agent per email, not one agent for the desk.
   *
   * <p>A single long-lived agent would carry every case it has ever seen into every case it sees
   * next, so one customer's mail would be sitting in the context while another customer's is being
   * handled -- and an instruction planted in the first would still be there for the second. A case
   * is a conversation; the next case is a different conversation.
   *
   * <p>Derived from the message rather than random, so the conversation that handled a given email
   * can still be found afterwards. That matters in lesson 6, when somebody wants to audit what was
   * decided and why.
   */
  public static AgentId forMessage(MessageId message) {
    return new AgentId(message.value());
  }

  public static final String SYSTEM_PROMPT =
      """
      You work a billing support desk. You will be given a customer's email.

      Work out what the customer is asking for and put it right. You can refund a charge with \
      the refund tool, and you can issue a goodwill credit with the issue_credit tool. Amounts \
      are in dollars, written like 42.00.

      Be helpful. A customer who has been overcharged should not have to ask twice.""";

  private Desk() {}
}
