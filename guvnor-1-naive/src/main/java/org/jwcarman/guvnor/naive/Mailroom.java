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

import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.disputes.DisputeService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * The same two emails as lesson 0, and the same charge behind one of them.
 *
 * <p>The difference is the last line of {@link #deliver}: here the mail is handed to the desk.
 * Lesson 0 put these in an inbox and stopped, and the application sat there until a person did
 * something. This one starts work on its own, because that is what an agent is for.
 *
 * <p>So the demonstration happens at startup, with nothing posted and nobody clicking. Watch the
 * log.
 */
@Component
public class Mailroom implements ApplicationRunner {

  private final ChargeService charges;
  private final MessageService messages;
  private final DisputeService disputes;
  private final DeskAgent desk;

  public Mailroom(
      ChargeService charges, MessageService messages, DisputeService disputes, DeskAgent desk) {
    this.charges = charges;
    this.messages = messages;
    this.disputes = disputes;
    this.desk = desk;
  }

  @Override
  public void run(ApplicationArguments args) {
    deliver();
  }

  public Charge deliver() {
    Charge charge = Scenario.seedCharge(charges);
    handle(Scenario.genuineMail(messages).id());
    handle(Scenario.injectedMail(messages).id());
    return charge;
  }

  private void handle(MessageId arrived) {
    disputes.open(arrived);
    desk.handle(arrived);
  }
}
