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
package org.jwcarman.guvnor.domain.correspondence;

import java.util.Objects;
import java.util.UUID;
import org.jwcarman.guvnor.domain.billing.Ids;

/** Identifies one message. */
public record MessageId(UUID value) {

  public MessageId {
    Objects.requireNonNull(value, "an id has a value");
  }

  public static MessageId next() {
    return new MessageId(Ids.next());
  }

  @Override
  public String toString() {
    return value.toString();
  }
}
