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
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * One customer, with one charge on their statement.
 *
 * <p>All the desk starts with, and all it needs: something for a dispute to be about. Every lesson
 * gets the same $42.00 charge, so a credit endorsed against it in lesson 4 is endorsed against the
 * same thing everywhere else.
 *
 * <p>No mail arrives at startup. An earlier version delivered the scenario emails here and the desk
 * went to work before anyone had opened the page, which meant every run began with a ledger that
 * already had money in it and a console that had already said the interesting part. The page has a
 * box for writing an email; that is a better way in, because the reader chooses what to send and
 * can change it.
 */
@Order(0)
public class OpeningBalance implements ApplicationRunner {

  private final ChargeService charges;

  public OpeningBalance(ChargeService charges) {
    this.charges = charges;
  }

  @Override
  public void run(ApplicationArguments args) {
    seed();
  }

  public Charge seed() {
    return Scenario.seedCharge(charges);
  }
}
