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
 * The bounded authority: money returned against a charge.
 *
 * <p>A refund cannot exceed the charge it names, less whatever has already been given back. That is
 * arithmetic rather than policy, so no amount of governance above this makes it safer and no
 * absence of governance makes it dangerous. It is here to be the thing the other authority is not.
 */
public class RefundService {

  private final ChargeService charges;
  private final LedgerService ledger;

  public RefundService(ChargeService charges, LedgerService ledger) {
    this.charges = charges;
    this.ledger = ledger;
  }

  public Refund issue(ChargeId chargeId, Money amount) {
    if (!amount.isPositive()) {
      throw new RefundRefused("a refund is for a positive amount, not " + amount);
    }
    Charge charge =
        charges.find(chargeId).orElseThrow(() -> new RefundRefused("no such charge: " + chargeId));

    Money alreadyGivenBack =
        ledger.refundedAgainst(chargeId, Money.zero(charge.amount().currency()));
    Money remaining = charge.amount().minus(alreadyGivenBack);
    if (amount.isGreaterThan(remaining)) {
      throw new RefundRefused(
          "%s is more than the %s still refundable against a charge of %s"
              .formatted(amount, remaining, charge.amount()));
    }

    Refund refund = new Refund(RefundId.next(), chargeId, amount, Instant.now());
    ledger.record(charge.account(), amount, LedgerEntry.Kind.REFUND, chargeId.toString());
    return refund;
  }
}
