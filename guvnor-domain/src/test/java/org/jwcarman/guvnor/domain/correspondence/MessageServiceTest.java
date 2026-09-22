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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.AccountId;

@DisplayName("Correspondence")
class MessageServiceTest {

  /**
   * The seam the rest of the series depends on. If a body ever becomes a field on Message, every
   * later lesson's protection can be walked around by reading the record, and nothing would say so.
   * This test is here to make that a build failure rather than a discovery.
   */
  @Test
  void keeps_the_body_off_the_record() {
    assertThat(Message.class.getRecordComponents())
        .extracting(RecordComponent::getName)
        .containsExactly("id", "from", "subject", "receivedAt")
        .doesNotContain("body");

    assertThat(Arrays.stream(Message.class.getMethods()).map(java.lang.reflect.Method::getName))
        .doesNotContain("body");
  }

  @Test
  void lists_what_arrived_without_reading_any_of_it() {
    MessageService messages = new MessageService();
    AccountId from = AccountId.next();
    messages.receive(from, "Duplicate charge", "the body nobody needed to list this");

    assertThat(messages.inbox()).hasSize(1);
    Assertions.assertThat(messages.inbox().getFirst().subject()).isEqualTo("Duplicate charge");
  }

  @Test
  void hands_out_a_body_only_when_asked_for_one() {
    MessageService messages = new MessageService();
    Message arrived = messages.receive(AccountId.next(), "Hello", "what they wrote");

    assertThat(messages.body(arrived.id())).contains("what they wrote");
  }

  @Test
  void has_no_body_for_a_message_it_never_saw() {
    MessageService messages = new MessageService();

    assertThat(messages.body(MessageId.next())).isEmpty();
    assertThat(messages.find(MessageId.next())).isEmpty();
  }
}
