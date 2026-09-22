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

import java.util.Currency;
import java.util.Objects;

/**
 * An amount of money, held in the currency's minor units.
 *
 * <p>Minor units rather than a decimal, because a support desk adds and subtracts money all day and
 * never divides it. There is no rounding policy here because there is nothing to round.
 */
public record Money(long minorUnits, Currency currency) implements Comparable<Money> {

  public Money {
    Objects.requireNonNull(currency, "money has a currency");
  }

  public static Money of(long minorUnits, Currency currency) {
    return new Money(minorUnits, currency);
  }

  /** Sterling, which is what this desk bills in. */
  public static Money gbp(long minorUnits) {
    return new Money(minorUnits, Currency.getInstance("GBP"));
  }

  public static Money zero(Currency currency) {
    return new Money(0L, currency);
  }

  public Money plus(Money other) {
    return new Money(minorUnits + sameCurrencyAs(other).minorUnits(), currency);
  }

  public Money minus(Money other) {
    return new Money(minorUnits - sameCurrencyAs(other).minorUnits(), currency);
  }

  public boolean isGreaterThan(Money other) {
    return compareTo(sameCurrencyAs(other)) > 0;
  }

  public boolean isPositive() {
    return minorUnits > 0L;
  }

  @Override
  public int compareTo(Money other) {
    return Long.compare(minorUnits, sameCurrencyAs(other).minorUnits());
  }

  /**
   * Refuses arithmetic across currencies.
   *
   * <p>Not a hypothetical: a desk that quietly added pence to cents would report a wrong balance
   * rather than fail, and nobody would find out from the total.
   */
  private Money sameCurrencyAs(Money other) {
    if (!currency.equals(other.currency())) {
      throw new IllegalArgumentException(
          "cannot mix %s and %s"
              .formatted(currency.getCurrencyCode(), other.currency().getCurrencyCode()));
    }
    return other;
  }

  @Override
  public String toString() {
    return "%d.%02d %s"
        .formatted(minorUnits / 100, Math.abs(minorUnits % 100), currency.getCurrencyCode());
  }
}
