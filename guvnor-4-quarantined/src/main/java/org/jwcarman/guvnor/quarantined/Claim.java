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
package org.jwcarman.guvnor.quarantined;

import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.loch.SurrogateType;

/**
 * What somebody says should happen to somebody's money.
 *
 * <p>A claim is not a decision. This one was produced by a model reading a stranger's email, so it
 * starts life unendorsed and stays that way until something the desk already trusts agrees with it.
 */
public record Claim(AccountId account, Money amount, String basis) {

  /** What a model proposes: an account and an amount, and no account of why. */
  public static Claim proposed(AccountId account, Money amount) {
    return new Claim(account, amount, "");
  }

  public static final SurrogateType<Claim> TYPE = SurrogateType.of("claim", Claim.class);
}
