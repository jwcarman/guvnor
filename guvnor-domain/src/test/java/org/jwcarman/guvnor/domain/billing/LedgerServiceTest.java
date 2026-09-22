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

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("The ledger")
class LedgerServiceTest {

  /**
   * A refund and a credit are authorised in completely different ways. They land here the same,
   * which is what makes "did any money leave" a question no arrangement above can dress up.
   */
  @Test
  void records_both_authorities_the_same_way() {
    ChargeService charges = new ChargeService();
    LedgerService ledger = new LedgerService();
    RefundService refunds = new RefundService(charges, ledger);
    CreditService credits = new CreditService(ledger);

    AccountId account = AccountId.next();
    ChargeId charge = ChargeId.next();
    charges.record(new Charge(charge, account, Money.usd(4_200L), "ACME", Instant.now()));

    refunds.issue(charge, Money.usd(4_200L));
    credits.issue(account, Money.usd(500L), "sorry");

    assertThat(ledger.entries())
        .hasSize(2)
        .extracting(LedgerEntry::kind)
        .containsExactly(LedgerEntry.Kind.REFUND, LedgerEntry.Kind.CREDIT);
  }

  @Test
  void counts_refunds_per_charge_and_credits_per_account() {
    LedgerService ledger = new LedgerService();
    AccountId account = AccountId.next();
    ChargeId charge = ChargeId.next();

    ledger.record(account, Money.usd(100L), LedgerEntry.Kind.REFUND, charge.toString());
    ledger.record(account, Money.usd(700L), LedgerEntry.Kind.CREDIT, "goodwill");

    assertThat(ledger.refundedAgainst(charge, Money.usd(0L))).isEqualTo(Money.usd(100L));
    assertThat(ledger.creditedTo(account, Money.usd(0L))).isEqualTo(Money.usd(700L));
  }

  @Test
  void is_empty_when_nothing_has_happened() {
    LedgerService ledger = new LedgerService();

    assertThat(ledger.entries()).isEmpty();
    assertThat(ledger.creditedTo(AccountId.next(), Money.usd(0L))).isEqualTo(Money.usd(0L));
  }
}
