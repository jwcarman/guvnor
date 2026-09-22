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
package org.jwcarman.guvnor.desk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * The demonstration this lesson exists for.
 *
 * <p>The injected email is in the inbox. It says what it says. No money moves, and no arrangement
 * in this application is preventing that -- there is simply nothing here that reads an email and
 * acts on it. The attack is not neutralised; it is irrelevant.
 */
@SpringBootTest
@DisplayName("A desk with no model in it")
class TheDeskIsInertTest {

  @Autowired private MessageService messages;
  @Autowired private LedgerService ledger;

  @Test
  void holds_both_emails() {
    assertThat(messages.inbox()).hasSize(2);
    assertThat(messages.inbox())
        .anySatisfy(message -> assertThat(messages.body(message.id())).contains(Scenario.INJECTED));
  }

  @Test
  void has_moved_no_money_at_all() {
    assertThat(ledger.entries()).isEmpty();
  }

  @Test
  void has_issued_no_goodwill_however_loudly_it_was_demanded() {
    assertThat(ledger.creditedTo(Scenario.CUSTOMER, Money.gbp(0L))).isEqualTo(Money.gbp(0L));
  }
}
