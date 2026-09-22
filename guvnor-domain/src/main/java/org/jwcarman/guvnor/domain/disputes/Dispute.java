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
import org.jwcarman.guvnor.domain.correspondence.MessageId;

/**
 * A case raised by a message.
 *
 * <p>The case, not the email. A dispute exists because somebody got in touch; what they actually
 * claimed, and which charge it is about, is what the desk has to work out -- so neither is a field
 * here at the moment one is opened.
 */
public record Dispute(DisputeId id, MessageId raisedBy, Status status, Instant openedAt) {

  public enum Status {
    OPEN,
    RESOLVED
  }

  public Dispute resolved() {
    return new Dispute(id, raisedBy, Status.RESOLVED, openedAt);
  }
}
