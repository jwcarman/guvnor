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

import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Money")
class MoneyTest {

  private static final Currency USD = Currency.getInstance("USD");

  @Nested
  @DisplayName("in one currency")
  class InOneCurrency {

    @Test
    void adds_and_subtracts_in_minor_units() {
      assertThat(Money.gbp(4_200L).plus(Money.gbp(800L))).isEqualTo(Money.gbp(5_000L));
      assertThat(Money.gbp(4_200L).minus(Money.gbp(200L))).isEqualTo(Money.gbp(4_000L));
    }

    @Test
    void compares_by_amount() {
      assertThat(Money.gbp(99_900L).isGreaterThan(Money.gbp(4_200L))).isTrue();
      assertThat(Money.gbp(4_200L).isGreaterThan(Money.gbp(4_200L))).isFalse();
    }

    @Test
    void prints_the_way_a_statement_would() {
      assertThat(Money.gbp(4_200L)).hasToString("42.00 GBP");
      assertThat(Money.gbp(99_900L)).hasToString("999.00 GBP");
      assertThat(Money.gbp(5L)).hasToString("0.05 GBP");
    }
  }

  @Nested
  @DisplayName("across currencies")
  class AcrossCurrencies {

    @Test
    void refuses_to_do_arithmetic_at_all() {
      Money pounds = Money.gbp(4_200L);
      Money dollars = Money.of(4_200L, USD);

      assertThatThrownBy(() -> pounds.plus(dollars))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("GBP")
          .hasMessageContaining("USD");
    }

    @Test
    void refuses_rather_than_reporting_a_wrong_total() {
      Money pounds = Money.gbp(4_200L);
      Money dollars = Money.of(100L, USD);

      assertThatThrownBy(() -> pounds.minus(dollars)).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
