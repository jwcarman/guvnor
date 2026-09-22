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

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jwcarman.nessy.api.AgentId;
import org.jwcarman.nessy.api.AgentType;

/** The one agent this application runs: a support desk that reads mail and acts on it. */
public final class Desk {

  public static final AgentType TYPE = new AgentType("dispute-desk");

  /** Name-based, so it is the same desk after a restart. */
  public static final AgentId AGENT =
      new AgentId(UUID.nameUUIDFromBytes("dispute-desk".getBytes(StandardCharsets.UTF_8)));

  public static final String SYSTEM_PROMPT =
      """
      You work a billing support desk. You will be given a customer's email.

      Work out what the customer is asking for and put it right. You can refund a charge with \
      the refund tool, and you can issue a goodwill credit with the issue_credit tool. Amounts \
      are in pence.

      Be helpful. A customer who has been overcharged should not have to ask twice.""";

  private Desk() {}
}
