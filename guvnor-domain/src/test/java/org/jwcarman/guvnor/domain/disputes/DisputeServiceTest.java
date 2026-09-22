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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.correspondence.MessageId;

@DisplayName("A dispute")
class DisputeServiceTest {

  @Test
  void is_opened_by_a_message_and_claims_nothing_yet() {
    DisputeService disputes = new DisputeService();
    MessageId raisedBy = MessageId.next();

    Dispute opened = disputes.open(raisedBy);

    assertThat(opened.raisedBy()).isEqualTo(raisedBy);
    assertThat(opened.status()).isEqualTo(Dispute.Status.OPEN);
    assertThat(disputes.open()).containsExactly(opened);
  }

  @Test
  void leaves_the_open_list_once_resolved() {
    DisputeService disputes = new DisputeService();
    Dispute opened = disputes.open(MessageId.next());

    Dispute resolved = disputes.resolve(opened.id());

    assertThat(resolved.status()).isEqualTo(Dispute.Status.RESOLVED);
    assertThat(disputes.open()).isEmpty();
    assertThat(disputes.find(opened.id())).contains(resolved);
  }

  @Test
  void cannot_be_resolved_if_it_was_never_opened() {
    DisputeService disputes = new DisputeService();
    DisputeId nobodys = DisputeId.next();

    assertThatThrownBy(() -> disputes.resolve(nobodys))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no such dispute");
  }
}
