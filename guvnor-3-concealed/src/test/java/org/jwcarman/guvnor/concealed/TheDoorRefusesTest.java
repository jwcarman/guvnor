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
package org.jwcarman.guvnor.concealed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Revealed;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.spi.inference.InferenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * What the charter refuses, asserted without a model anywhere.
 *
 * <p>This is the difference between lesson 2 and lesson 3, stated as tests. Lesson 2's assertions
 * were about whether a rule noticed something. These are about what is possible.
 *
 * <p>Note that every one of them runs without asking a model anything. The context needs a provider
 * because the application has an agent in it, and not one assertion here involves that agent. That
 * is what it means for a protection to be structural rather than judged.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Mail that has been concealed")
class TheDoorRefusesTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

  @TestConfiguration
  static class NoModelNeeded {
    @Bean
    InferenceProvider silent() {
      return new RecordingProvider();
    }
  }

  @Autowired private Conceal<String> inbound;
  @Autowired private Reveal<String> model;
  @Autowired private Derivation<String, String> redacted;

  @Test
  void cannot_be_read_by_the_model_as_it_arrived() {
    Surrogate<String> mail = inbound.conceal(Scenario.GENUINE);

    assertThat(model.reveal(mail)).isInstanceOf(Revealed.Denied.class);
  }

  /**
   * The point. Lesson 2 asked "does this text look like it has a card in it". Nothing here looks at
   * the text: an email with no card in it is refused just the same, because the question is what
   * channel it arrived on, not what it happens to say.
   */
  @Test
  void is_refused_even_when_it_contains_no_card_at_all() {
    Surrogate<String> harmless = inbound.conceal("Hello, just saying thanks for the help.");

    assertThat(model.reveal(harmless)).isInstanceOf(Revealed.Denied.class);
  }

  @Test
  void can_be_read_after_the_one_declared_declassification() {
    Surrogate<String> safe = redacted.derive(inbound.conceal(Scenario.GENUINE)).orThrow();

    assertThat(model.reveal(safe).orThrow()).contains("refund the duplicate charge");
  }

  @Test
  void no_longer_carries_the_card_however_it_was_written() {
    Surrogate<String> spaced =
        redacted.derive(inbound.conceal(Scenario.GENUINE_SPACED_CARD)).orThrow();
    Surrogate<String> unspaced = redacted.derive(inbound.conceal(Scenario.GENUINE)).orThrow();

    assertThat(model.reveal(spaced).orThrow()).doesNotContain("4111").contains("[card redacted]");
    assertThat(model.reveal(unspaced).orThrow()).doesNotContain("4111").contains("[card redacted]");
  }

  /**
   * And the thing that has not been fixed.
   *
   * <p>The injected instruction is not cardholder data, so redaction leaves it exactly where it
   * was. Everything this lesson added is about confidentiality, and this sentence is about
   * something else entirely.
   */
  @Test
  void still_carries_every_instruction_the_customer_wrote() {
    Surrogate<String> safe = redacted.derive(inbound.conceal(Scenario.INJECTED_POLITELY)).orThrow();

    assertThat(model.reveal(safe).orThrow()).contains("999.00").contains("goodwill credit");
  }
}
