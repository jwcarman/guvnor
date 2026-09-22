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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("A goodwill credit")
class CreditServiceTest {

  private LedgerService ledger;
  private CreditService credits;
  private AccountId account;

  @BeforeEach
  void aDesk() {
    ledger = new LedgerService();
    credits = new CreditService(ledger);
    account = AccountId.next();
  }

  /**
   * The point of this class, stated as a test: there is no charge to net against, so there is no
   * arithmetic that could bound it. This is what every lesson in the series is trying to protect,
   * and it is why protection has to live above the billing system rather than inside it.
   */
  @Test
  void is_bounded_by_nothing_in_the_billing_system() {
    Credit absurd = credits.issue(account, Money.usd(99_900L), "customer unhappy");

    assertThat(absurd.amount()).isEqualTo(Money.usd(99_900L));
    assertThat(ledger.creditedTo(account, Money.usd(0L))).isEqualTo(Money.usd(99_900L));
  }

  @Test
  void accumulates_because_nothing_is_keeping_count_against_it() {
    credits.issue(account, Money.usd(99_900L), "first");
    credits.issue(account, Money.usd(99_900L), "second");

    assertThat(ledger.creditedTo(account, Money.usd(0L))).isEqualTo(Money.usd(199_800L));
  }

  @Test
  void names_a_reason_rather_than_a_charge() {
    Credit credit = credits.issue(account, Money.usd(500L), "late delivery");

    assertThat(credit.reason()).isEqualTo("late delivery");
    assertThat(credit.account()).isEqualTo(account);
  }

  @Test
  void is_for_a_positive_amount() {
    assertThatThrownBy(() -> credits.issue(account, Money.usd(-100L), "oops"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
