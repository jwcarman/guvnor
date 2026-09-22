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
 * What a claim can and cannot reach, with no model involved in any of it.
 *
 * <p>Lessons 1 to 3 could only assert what the model was handed. From here the interesting
 * assertions are about what is reachable, and a model is not required to make them — which is the
 * point. A protection that needs a model to demonstrate it is a protection that depends on one.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("A claim a model made")
class AnUnendorsedClaimTest {

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

  @TestConfiguration
  static class NoModelNeeded {
    @Bean
    InferenceProvider silent() {
      return new RecordingProvider();
    }
  }

  @Autowired private Conceal<Claim> proposed;
  @Autowired private Derivation<Claim, Claim> confirmed;
  @Autowired private Reveal<Claim> authority;
  @Autowired private ChargeService charges;

  private Surrogate<Claim> claimFor(Money amount) {
    return proposed.conceal(new Claim(Scenario.CUSTOMER, amount, Request.Kind.GOODWILL_CREDIT, ""));
  }

  @Test
  void cannot_reach_the_authority_that_moves_money() {
    Surrogate<Claim> claim = claimFor(Money.usd(99_900L));

    assertThat(authority.reveal(claim)).isInstanceOf(Revealed.Denied.class);
  }

  /**
   * The sentence that makes lessons 1 to 3 unnecessary.
   *
   * <p>A modest claim is refused exactly as an outrageous one is. The door is not reading the
   * amount, it is reading who vouched for it, and nobody has.
   */
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

    Derived<Claim> endorsed = confirmed.derive(claimFor(charge.amount()));

    assertThat(endorsed).isInstanceOf(Derived.Made.class);
  }

  @Test
  void reaches_the_authority_only_after_that_endorsement() {
    Charge charge = Scenario.seedCharge(charges);
    Surrogate<Claim> claim = claimFor(charge.amount());

    assertThat(authority.reveal(claim)).isInstanceOf(Revealed.Denied.class);

    Surrogate<Claim> supported = ((Derived.Made<Claim>) confirmed.derive(claim)).value();
    assertThat(authority.reveal(supported).orThrow().amount()).isEqualTo(charge.amount());
  }

  /**
   * And the thing that has not changed, stated plainly.
   *
   * <p>Nothing here stops a model being persuaded. The injected email still arrives, the model may
   * still believe it, and it may still call the tool asking for $999.00. What changed is that
   * believing it is no longer sufficient.
   */
  @Test
  void can_still_be_made_for_any_amount_a_persuaded_model_likes() {
    Surrogate<Claim> absurd = claimFor(Money.usd(99_900L));

    assertThat(absurd).isNotNull();
    assertThat(authority.reveal(absurd)).isInstanceOf(Revealed.Denied.class);
  }
}
