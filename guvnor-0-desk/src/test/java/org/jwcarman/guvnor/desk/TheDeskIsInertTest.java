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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The demonstration this lesson exists for.
 *
 * <p>The injected email is delivered to a desk that announces every arrival, exactly as every later
 * lesson does. No money moves, and no arrangement here is preventing that -- there is simply
 * nothing listening. The attack is not neutralised; it is irrelevant.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("A desk with no model in it")
class TheDeskIsInertTest {

  @Autowired private MockMvc http;
  @Autowired private MessageService messages;
  @Autowired private LedgerService ledger;

  private void deliver(String body) throws Exception {
    http.perform(post("/mail").param("subject", "Refund request").param("body", body));
  }

  @Test
  void takes_the_email_and_keeps_it() throws Exception {
    deliver(Scenario.INJECTED);

    assertThat(messages.inbox()).hasSize(1);
    assertThat(messages.body(messages.inbox().getFirst().id())).contains(Scenario.INJECTED);
  }

  @Test
  void moves_no_money_at_all() throws Exception {
    deliver(Scenario.INJECTED);

    assertThat(ledger.entries()).isEmpty();
  }

  @Test
  void issues_no_goodwill_however_loudly_it_is_demanded() throws Exception {
    deliver(Scenario.INJECTED);
    deliver(Scenario.INJECTED_POLITELY);

    assertThat(ledger.creditedTo(Scenario.CUSTOMER, Money.usd(0L))).isEqualTo(Money.usd(0L));
  }
}
