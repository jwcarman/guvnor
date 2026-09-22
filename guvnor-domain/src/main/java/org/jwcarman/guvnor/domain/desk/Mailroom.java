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
package org.jwcarman.guvnor.domain.desk;

import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.disputes.DisputeService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * The two emails, delivered to every lesson.
 *
 * <p>Identical everywhere on purpose. A reader comparing lesson 4 against lesson 1 needs to know
 * that the inputs were the same, and the cheapest way to know it is for there to be one copy of the
 * code that delivers them.
 *
 * <p>A lesson that needs further mail to make its point delivers it itself -- lesson 2 does, to
 * show a filter being walked past -- but these two arrive in all of them.
 *
 * <p>Note what this class does not do: it stores mail and opens cases, and then it is finished.
 * Whether anything reads what arrived is not its business.
 */
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
