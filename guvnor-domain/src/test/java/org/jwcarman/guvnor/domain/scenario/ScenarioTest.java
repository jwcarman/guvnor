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
package org.jwcarman.guvnor.domain.scenario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.billing.RefundRefused;
import org.jwcarman.guvnor.domain.billing.RefundService;

@DisplayName("The scenario every lesson runs")
class ScenarioTest {

  /**
   * Why the attack asks for goodwill rather than a refund.
   *
   * <p>Had it asked for a refund, the desk would refuse it here, in lesson 0, with no governance
   * anywhere -- and every later lesson would be claiming credit for arithmetic. This test exists so
   * that if somebody ever "simplifies" the scenario back to a refund, the reason it was not a
   * refund is stated in the failure.
   */
  @Test
  void the_attack_would_be_pointless_against_the_bounded_authority() {
    ChargeService charges = new ChargeService();
    LedgerService ledger = new LedgerService();
    RefundService refunds = new RefundService(charges, ledger);
    Charge charge = Scenario.seedCharge(charges);

    assertThatThrownBy(() -> refunds.issue(charge.id(), Scenario.INJECTED_AMOUNT))
        .isInstanceOf(RefundRefused.class);

    assertThat(ledger.entries()).isEmpty();
  }

  /** And why it is aimed where it is: nothing in the desk says no to this. */
  @Test
  void the_attack_succeeds_outright_against_the_unbounded_one() {
    LedgerService ledger = new LedgerService();
    CreditService credits = new CreditService(ledger);

    credits.issue(Scenario.CUSTOMER, Scenario.INJECTED_AMOUNT, "a chargeback has been filed");

    assertThat(ledger.creditedTo(Scenario.CUSTOMER, Money.gbp(0L)))
        .isEqualTo(Scenario.INJECTED_AMOUNT);
  }

  @Test
  void the_genuine_mail_carries_cardholder_data_because_real_mail_does() {
    assertThat(Scenario.GENUINE).contains("4111111111114821");
  }

  @Test
  void the_injected_mail_asks_for_the_unbounded_authority_by_name() {
    assertThat(Scenario.INJECTED).contains("goodwill credit").contains("999.00");
  }
}
