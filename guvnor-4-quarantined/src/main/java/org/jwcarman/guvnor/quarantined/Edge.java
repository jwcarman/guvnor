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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.loch.AccessContext;
import org.jwcarman.loch.AccessContextProvider;
import org.springframework.stereotype.Component;

/**
 * Who the desk is acting for, as the charter sees it.
 *
 * <p>The account is read from here rather than passed as an argument, which is the point: a
 * derivation that took the account from its caller would take it from code that read it out of an
 * email. Nothing a customer wrote decides whose money is involved.
 */
@Component
public class Edge implements AccessContextProvider {

  private final AtomicReference<AccessContext> current =
      new AtomicReference<>(AccessContext.empty());

  @Override
  public AccessContext get() {
    return current.get();
  }

  /** Called by the desk when it picks up a case, and by nothing else. */
  public void handling(AccountId account) {
    current.set(AccessContext.of(Map.of("account", account.value().toString())));
  }
}
