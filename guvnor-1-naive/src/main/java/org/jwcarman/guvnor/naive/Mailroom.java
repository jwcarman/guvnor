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
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.disputes.DisputeService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** The same two emails as lesson 0, and the same charge behind one of them. */
@Component
public class Mailroom implements ApplicationRunner {

  private final ChargeService charges;
  private final MessageService messages;
  private final DisputeService disputes;

  public Mailroom(ChargeService charges, MessageService messages, DisputeService disputes) {
    this.charges = charges;
    this.messages = messages;
    this.disputes = disputes;
  }

  @Override
  public void run(ApplicationArguments args) {
    deliver();
  }

  public Charge deliver() {
    Charge charge = Scenario.seedCharge(charges);
    disputes.open(Scenario.genuineMail(messages).id());
    disputes.open(Scenario.injectedMail(messages).id());
    return charge;
  }
}
