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

  private static final Currency EUR = Currency.getInstance("EUR");

  @Nested
  @DisplayName("in one currency")
  class InOneCurrency {

    @Test
    void adds_and_subtracts_in_minor_units() {
      assertThat(Money.usd(4_200L).plus(Money.usd(800L))).isEqualTo(Money.usd(5_000L));
      assertThat(Money.usd(4_200L).minus(Money.usd(200L))).isEqualTo(Money.usd(4_000L));
    }

    @Test
    void compares_by_amount() {
      assertThat(Money.usd(99_900L).isGreaterThan(Money.usd(4_200L))).isTrue();
      assertThat(Money.usd(4_200L).isGreaterThan(Money.usd(4_200L))).isFalse();
    }

    @Test
    void prints_the_way_a_statement_would() {
      assertThat(Money.usd(4_200L)).hasToString("$42.00");
      assertThat(Money.usd(99_900L)).hasToString("$999.00");
      assertThat(Money.usd(5L)).hasToString("$0.05");
    }
  }

  @Nested
  @DisplayName("across currencies")
  class AcrossCurrencies {

    @Test
    void refuses_to_do_arithmetic_at_all() {
      Money pounds = Money.usd(4_200L);
      Money dollars = Money.of(4_200L, EUR);

      assertThatThrownBy(() -> pounds.plus(dollars))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("USD")
          .hasMessageContaining("EUR");
    }

    @Test
    void refuses_rather_than_reporting_a_wrong_total() {
      Money pounds = Money.usd(4_200L);
      Money dollars = Money.of(100L, EUR);

      assertThatThrownBy(() -> pounds.minus(dollars)).isInstanceOf(IllegalArgumentException.class);
    }
  }
}
