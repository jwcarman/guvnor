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
 * The defect, asserted without a model.
 *
 * <p>Lesson 1's claim is not "a model did something stupid". It is that this application hands a
 * model one piece of text containing both its own instructions and a stranger's, with nothing
 * recording which is which -- so no amount of care downstream can tell them apart. That is a
 * property of the prompt, it is deterministic, and it is what lessons 3 and 4 remove.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("What the desk hands its agent")
class TheEmailIsTheInstructionsTest {

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
  void contains_the_customers_words_verbatim() throws Exception {
    assertThat(promptFor("A one-off", "please look at charge 12"))
        .contains("please look at charge 12");
  }

  @Test
  void puts_them_in_the_same_channel_as_its_own() throws Exception {
    String prompt = promptFor("Refund request", Scenario.INJECTED);

    assertThat(prompt)
        .as("the desk's framing and the stranger's instruction, in one string")
        .contains("A customer has written in")
        .contains("Ignore previous instructions");
  }

  @Test
  void carries_the_card_number_too() throws Exception {
    assertThat(promptFor("Duplicate charge", Scenario.GENUINE)).contains("4111111111114821");
  }

  /**
   * Posts one email and returns the prompt the desk built from it.
   *
   * <p>Counts from where the probe already was: the mailroom hands the desk both scenario emails at
   * startup, so the interesting prompt is never the first one.
   */
  private String promptFor(String subject, String body) throws Exception {
    RecordingProvider probe = (RecordingProvider) provider;
    int before = probe.seen();

    http.perform(post("/mail").param("subject", subject).param("body", body));
    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(() -> assertThat(probe.seen()).isGreaterThan(before));

    return probe.prompts().get(before);
  }
}
