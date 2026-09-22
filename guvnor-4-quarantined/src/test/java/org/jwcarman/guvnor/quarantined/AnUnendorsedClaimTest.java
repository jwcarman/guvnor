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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.Derived;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Revealed;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.api.Usage;
import org.jwcarman.nessy.api.extraction.Extraction;
import org.jwcarman.nessy.api.extraction.Extractor;
import org.jwcarman.nessy.spi.inference.InferenceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * What a claim can and cannot reach, with no real model anywhere.
 *
 * <p>The extractor here is a stub that returns whatever it is told to, because what is being tested
 * is the arrangement rather than a model's judgement. That is the claim of this lesson stated as a
 * test setup: if the protection needed a model to demonstrate it, it would be a protection that
 * depends on one.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("A claim a model made")
class AnUnendorsedClaimTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

  /** Whatever the "model" is told to have read, this run. */
  static final ThreadLocal<Request> READ = new ThreadLocal<>();

  @TestConfiguration
  static class NoModelNeeded {
    @Bean
    InferenceProvider silent() {
      return new RecordingProvider();
    }

    @Bean
    @Primary
    Extractor stubbed() {
      return new Extractor() {
        @Override
        public <T> Extraction<T> extract(Class<T> type, String document) {
          return new Extraction.Extracted<>(type.cast(READ.get()), Usage.unknown());
        }
      };
    }
  }

  @Autowired private Conceal<String> inbound;
  @Autowired private Derivation<String, Claim> quarantined;
  @Autowired private Derivation<Claim, Claim> confirmed;
  @Autowired private Reveal<Claim> authority;
  @Autowired private ChargeService charges;
  @Autowired private Edge edge;

  @BeforeEach
  void handlingOurCustomer() {
    edge.handling(Scenario.CUSTOMER);
  }

  private Surrogate<Claim> claimFor(Money amount) {
    READ.set(new Request(Request.Kind.GOODWILL_CREDIT, amount.toDollars().toPlainString()));
    Surrogate<String> mail = inbound.conceal("whatever the customer wrote");
    return ((Derived.Made<Claim>) quarantined.derive(mail)).value();
  }

  @Test
  void cannot_reach_the_authority_that_moves_money() {
    assertThat(authority.reveal(claimFor(Money.usd(99_900L)))).isInstanceOf(Revealed.Denied.class);
  }

  /** The door is not reading the amount. It is reading who vouched for it, and nobody has. */
  @Test
  void is_refused_at_any_size_at_all() {
    assertThat(authority.reveal(claimFor(Money.usd(1L)))).isInstanceOf(Revealed.Denied.class);
    assertThat(authority.reveal(claimFor(Money.usd(99_900L)))).isInstanceOf(Revealed.Denied.class);
  }

  @Test
  void is_not_endorsed_when_no_charge_supports_it() {
    Scenario.seedCharge(charges);

    assertThat(confirmed.derive(claimFor(Money.usd(99_900L)))).isNotInstanceOf(Derived.Made.class);
  }

  @Test
  void is_endorsed_when_a_real_charge_agrees_with_it() {
    Charge charge = Scenario.seedCharge(charges);

    assertThat(confirmed.derive(claimFor(charge.amount()))).isInstanceOf(Derived.Made.class);
  }

  @Test
  void reaches_the_authority_only_after_that_endorsement() {
    Charge charge = Scenario.seedCharge(charges);
    Surrogate<Claim> claim = claimFor(charge.amount());

    assertThat(authority.reveal(claim)).isInstanceOf(Revealed.Denied.class);

    Surrogate<Claim> supported = ((Derived.Made<Claim>) confirmed.derive(claim)).value();
    assertThat(authority.reveal(supported).orThrow().supportedBy()).isEqualTo(charge.id());
  }

  /** Nothing on a claim can hold a sentence, so nothing filling one in can smuggle one. */
  @Test
  void has_no_text_on_it_at_all() {
    assertThat(Claim.class.getRecordComponents())
        .extracting(java.lang.reflect.RecordComponent::getType)
        .doesNotContain(String.class);
  }
}
