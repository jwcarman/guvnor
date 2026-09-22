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
package org.jwcarman.guvnor.domain.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A refund")
class RefundServiceTest {

  private ChargeService charges;
  private LedgerService ledger;
  private RefundService refunds;
  private ChargeId charge;
  private AccountId account;

  @BeforeEach
  void aDeskWithOneChargeOnIt() {
    charges = new ChargeService();
    ledger = new LedgerService();
    refunds = new RefundService(charges, ledger);
    account = AccountId.next();
    charge = ChargeId.next();
    charges.record(new Charge(charge, account, Money.gbp(4_200L), "ACME", Instant.now()));
  }

  @Test
  void goes_back_against_the_charge_it_names() {
    Refund refund = refunds.issue(charge, Money.gbp(4_200L));

    assertThat(refund.charge()).isEqualTo(charge);
    assertThat(refund.amount()).isEqualTo(Money.gbp(4_200L));
    assertThat(ledger.refundedAgainst(charge, Money.gbp(0L))).isEqualTo(Money.gbp(4_200L));
  }

  /**
   * The invariant that shapes the whole series. It is arithmetic, not policy: no governance above
   * this makes it safer, and none of its absence makes it dangerous.
   */
  @Test
  void cannot_exceed_the_charge_however_it_was_asked_for() {
    assertThatThrownBy(() -> refunds.issue(charge, Money.gbp(99_900L)))
        .isInstanceOf(RefundRefused.class)
        .hasMessageContaining("999.00 GBP")
        .hasMessageContaining("42.00 GBP");

    assertThat(ledger.entries()).isEmpty();
  }

  @Test
  void cannot_exceed_what_is_left_after_an_earlier_one() {
    refunds.issue(charge, Money.gbp(3_000L));

    assertThatThrownBy(() -> refunds.issue(charge, Money.gbp(2_000L)))
        .isInstanceOf(RefundRefused.class)
        .hasMessageContaining("12.00 GBP");

    assertThat(ledger.refundedAgainst(charge, Money.gbp(0L))).isEqualTo(Money.gbp(3_000L));
  }

  @Test
  void needs_a_charge_that_exists() {
    ChargeId nobodys = ChargeId.next();

    assertThatThrownBy(() -> refunds.issue(nobodys, Money.gbp(100L)))
        .isInstanceOf(RefundRefused.class)
        .hasMessageContaining("no such charge");
  }

  @Test
  void is_for_a_positive_amount() {
    assertThatThrownBy(() -> refunds.issue(charge, Money.gbp(0L)))
        .isInstanceOf(RefundRefused.class);
  }
}
