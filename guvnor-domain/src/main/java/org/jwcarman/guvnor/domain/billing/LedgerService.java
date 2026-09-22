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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Every movement of money, and the only honest answer to "did anything actually happen".
 *
 * <p>A refund and a credit are authorised in completely different ways and land here the same, so a
 * question asked of the ledger cannot be fooled by how the money was let out.
 */
public class LedgerService {

  private final List<LedgerEntry> entries = new CopyOnWriteArrayList<>();

  void record(AccountId account, Money amount, LedgerEntry.Kind kind, String reference) {
    entries.add(new LedgerEntry(account, amount, kind, reference, Instant.now()));
  }

  public List<LedgerEntry> entries() {
    return List.copyOf(entries);
  }

  /** What has already been given back against one charge, which is what bounds the next refund. */
  public Money refundedAgainst(ChargeId charge, Money zero) {
    return entries.stream()
        .filter(entry -> entry.kind() == LedgerEntry.Kind.REFUND)
        .filter(entry -> entry.reference().equals(charge.toString()))
        .map(LedgerEntry::amount)
        .reduce(zero, Money::plus);
  }

  /** What has been handed to an account as goodwill. Nothing bounds this. */
  public Money creditedTo(AccountId account, Money zero) {
    return entries.stream()
        .filter(entry -> entry.kind() == LedgerEntry.Kind.CREDIT)
        .filter(entry -> entry.account().equals(account))
        .map(LedgerEntry::amount)
        .reduce(zero, Money::plus);
  }
}
