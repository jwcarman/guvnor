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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
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

  /**
   * How much this is, in whole currency units: 999.00 rather than 99900.
   *
   * <p>The JSON form on purpose. Everything that reads or writes money here -- a person, a page, a
   * model -- writes it the way it appears on a statement, and a type that serialises as its
   * internal representation asks every one of them to convert. Asking a language model to convert
   * is asking it to be wrong occasionally, which it duly was: the first version of the desk's tools
   * took a primitive number of cents and a model sent nothing at all for it.
   */
  @JsonValue
  public BigDecimal toDollars() {
    return BigDecimal.valueOf(minorUnits, 2);
  }

  /**
   * Money, from whatever was written where an amount belonged.
   *
   * <p>Takes text rather than a number so that a dollar sign, a thousands separator or a missing
   * decimal is a readable refusal instead of a deserialisation error the caller never sees. What
   * arrives here came from outside, and the useful failure is one that can be handed back.
   */
  @JsonCreator
  public static Money fromDollars(String written) {
    if (written == null || written.isBlank()) {
      throw new IllegalArgumentException("no amount was given; write it in dollars, like 42.00");
    }
    String cleaned = written.strip().replace("$", "").replace(",", "");
    try {
      return usd(
          new BigDecimal(cleaned)
              .movePointRight(2)
              .setScale(0, RoundingMode.HALF_UP)
              .longValueExact());
    } catch (ArithmeticException | NumberFormatException notAnAmount) {
      throw new IllegalArgumentException(
          "'" + written + "' is not an amount of dollars, like 42.00");
    }
  }

  public static Money of(long minorUnits, Currency currency) {
    return new Money(minorUnits, currency);
  }

  /** Dollars, which is what this desk bills in. */
  public static Money usd(long cents) {
    return new Money(cents, Currency.getInstance("USD"));
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
   * <p>Not a hypothetical: a desk that quietly added cents to cents would report a wrong balance
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
    String symbol = currency.getSymbol(Locale.US);
    return "%s%d.%02d".formatted(symbol, minorUnits / 100, Math.abs(minorUnits % 100));
  }
}
