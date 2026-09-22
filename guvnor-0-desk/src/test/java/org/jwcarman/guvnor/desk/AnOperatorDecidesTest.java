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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The other half: the authorities work, and a person reaches them.
 *
 * <p>Worth stating because "nothing happened" would otherwise be indistinguishable from a broken
 * application. The desk can move money. It just will not do so on its own.
 */
/*
 * The desk keeps its books in memory, and Spring caches the context between test classes, so a
 * refund made here would otherwise still be on the ledger when the inert test asks whether any
 * money has moved. That test is this lesson's entire claim, so it must never inherit another
 * test's spending.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("An operator at the desk")
class AnOperatorDecidesTest {

  @Autowired private MockMvc http;
  @Autowired private MessageService messages;
  @Autowired private ChargeService charges;
  @Autowired private LedgerService ledger;

  @Test
  void can_refund_the_charge_the_genuine_mail_is_about() throws Exception {
    Message mail = messages.inbox().getFirst();
    Charge charge = charges.forAccount(Scenario.CUSTOMER).getFirst();

    http.perform(
            post("/mail/{id}/refund", mail.id())
                .param("charge", charge.id().toString())
                .param("pence", "4200"))
        .andExpect(status().is3xxRedirection());

    assertThat(ledger.refundedAgainst(charge.id(), Money.gbp(0L))).isEqualTo(Money.gbp(4_200L));
  }

  @Test
  void is_refused_a_refund_larger_than_the_charge_without_anything_governing_them()
      throws Exception {
    Message mail = messages.inbox().getFirst();
    Charge charge = charges.forAccount(Scenario.CUSTOMER).getFirst();

    http.perform(
            post("/mail/{id}/refund", mail.id())
                .param("charge", charge.id().toString())
                .param("pence", "99900"))
        .andExpect(status().is3xxRedirection());

    assertThat(ledger.refundedAgainst(charge.id(), Money.gbp(0L))).isEqualTo(Money.gbp(0L));
  }

  @Test
  void can_issue_goodwill_of_any_size_at_all() throws Exception {
    Message mail = messages.inbox().getFirst();

    http.perform(
            post("/mail/{id}/credit", mail.id())
                .param("account", Scenario.CUSTOMER.value().toString())
                .param("pence", "99900")
                .param("reason", "operator decided"))
        .andExpect(status().is3xxRedirection());

    assertThat(ledger.creditedTo(Scenario.CUSTOMER, Money.gbp(0L))).isEqualTo(Money.gbp(99_900L));
  }
}
