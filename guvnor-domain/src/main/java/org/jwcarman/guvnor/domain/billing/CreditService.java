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

import java.time.Instant;

/**
 * The unbounded authority: goodwill money to an account.
 *
 * <p>There is no charge to net against and therefore no arithmetic that could bound it. A desk
 * needs this -- it is how an unhappy customer stops being unhappy -- and it is why governance has
 * to live above the billing system rather than inside it. Nothing this class could do would make it
 * safe, because "safe" here means "issued for a good reason", and a good reason is not a quantity.
 */
public class CreditService {

  private final LedgerService ledger;

  public CreditService(LedgerService ledger) {
    this.ledger = ledger;
  }

  public Credit issue(AccountId account, Money amount, String reason) {
    if (!amount.isPositive()) {
      throw new IllegalArgumentException("a credit is for a positive amount, not " + amount);
    }
    Credit credit = new Credit(CreditId.next(), account, amount, reason, Instant.now());
    ledger.record(account, amount, LedgerEntry.Kind.CREDIT, reason);
    return credit;
  }
}
