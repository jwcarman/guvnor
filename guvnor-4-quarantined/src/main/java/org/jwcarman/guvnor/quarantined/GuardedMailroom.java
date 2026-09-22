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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Surrogate;
import org.springframework.stereotype.Component;

/**
 * Where the guards go up, which is the moment mail arrives.
 *
 * <p>The desk's own message service keeps bodies in a map, as every mail system does. That is fine
 * for a desk with a person at it, and it is a window in a desk with a model in it: between the
 * email arriving and something deciding to be careful with it, the plaintext is an ordinary String
 * in ordinary storage, and anything at all could read it.
 *
 * <p>Mail to a billing desk may contain a card number. It certainly contains something a stranger
 * wrote. Both of those are true the instant it lands, not at the point somebody remembers, so the
 * concealment happens here: the body is handed to the charter and what is kept is a surrogate.
 *
 * <p>Nothing downstream can read it without naming where it is going. Including this class.
 */
@Component
public class GuardedMailroom {

  private final Conceal<String> inbound;
  private final Map<MessageId, Surrogate<String>> concealed = new ConcurrentHashMap<>();

  public GuardedMailroom(Conceal<String> inbound) {
    this.inbound = inbound;
  }

  /**
   * Takes custody of what arrived.
   *
   * <p>Called with the plaintext exactly once, at the edge, and never again. What it returns is the
   * only handle anything else in this application gets.
   */
  public Surrogate<String> take(MessageId id, String body) {
    Surrogate<String> surrogate = inbound.conceal(body);
    concealed.put(id, surrogate);
    return surrogate;
  }

  public Optional<Surrogate<String>> of(MessageId id) {
    return Optional.ofNullable(concealed.get(id));
  }
}
