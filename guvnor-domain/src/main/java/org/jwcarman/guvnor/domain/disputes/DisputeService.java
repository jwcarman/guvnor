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
package org.jwcarman.guvnor.domain.disputes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jwcarman.guvnor.domain.correspondence.MessageId;

/** The cases on the desk. */
public class DisputeService {

  private final Map<DisputeId, Dispute> disputes = new ConcurrentHashMap<>();

  public Dispute open(MessageId raisedBy) {
    Dispute dispute = new Dispute(DisputeId.next(), raisedBy, Dispute.Status.OPEN, Instant.now());
    disputes.put(dispute.id(), dispute);
    return dispute;
  }

  public Optional<Dispute> find(DisputeId id) {
    return Optional.ofNullable(disputes.get(id));
  }

  public List<Dispute> open() {
    return disputes.values().stream()
        .filter(dispute -> dispute.status() == Dispute.Status.OPEN)
        .toList();
  }

  public Dispute resolve(DisputeId id) {
    return disputes.compute(
        id,
        (key, existing) -> {
          if (existing == null) {
            throw new IllegalArgumentException("no such dispute: " + key);
          }
          return existing.resolved();
        });
  }
}
