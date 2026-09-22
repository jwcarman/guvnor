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
package org.jwcarman.guvnor.naive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.jwcarman.nessy.spi.inference.InferenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * One agent per email, and why it matters even in the lesson with no governance.
 *
 * <p>A desk that ran every case through one long-lived agent would carry each customer's mail into
 * the next customer's case. An instruction planted in one email would still be sitting in the
 * context while an unrelated one was handled -- an injection with a shelf life, reaching people who
 * were never sent it.
 *
 * <p>This is not governance. It is ordinary correctness, and it is the sort of thing that is nearly
 * free to get right at the start and very expensive to notice later.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Each email")
class EachEmailIsItsOwnConversationTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

  @TestConfiguration
  static class Probe {
    @Bean
    InferenceProvider recordingProvider() {
      return new RecordingProvider();
    }
  }

  @Autowired private MockMvc http;
  @Autowired private InferenceProvider provider;

  @Test
  void is_handled_without_the_previous_one_in_the_context() throws Exception {
    RecordingProvider probe = (RecordingProvider) provider;
    int before = probe.seen();

    http.perform(post("/mail").param("subject", "One").param("body", Scenario.INJECTED));
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(() -> assertThat(probe.seen()).isGreaterThan(before));

    int afterFirst = probe.seen();
    http.perform(post("/mail").param("subject", "Two").param("body", Scenario.GENUINE));
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(() -> assertThat(probe.seen()).isGreaterThan(afterFirst));

    String second = probe.prompts().get(afterFirst);
    assertThat(second).contains(Scenario.GENUINE);
    assertThat(second)
        .as("the first email's instructions must not still be in the room")
        .doesNotContain(Scenario.INJECTED);
  }
}
