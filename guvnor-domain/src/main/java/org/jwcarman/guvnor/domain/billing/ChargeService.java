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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** The charges this desk can see. */
public class ChargeService {

  private final Map<ChargeId, Charge> charges = new ConcurrentHashMap<>();

  public Charge record(Charge charge) {
    charges.put(charge.id(), charge);
    return charge;
  }

  public Optional<Charge> find(ChargeId id) {
    return Optional.ofNullable(charges.get(id));
  }

  public List<Charge> forAccount(AccountId account) {
    return charges.values().stream().filter(charge -> charge.account().equals(account)).toList();
  }
}
