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
package org.jwcarman.guvnor.careful;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.jwcarman.guvnor.domain.scenario.Scenario;

/**
 * Both defences, doing exactly what they were built to do.
 *
 * <p>This half has to come first, and has to be honest, or the lesson is a straw man. The team that
 * wrote these rules was not careless. They looked at what went wrong, they wrote something that
 * stops it, and it stops it.
 */
@DisplayName("The defences")
class TheDefencesWorkTest {

  @Nested
  @DisplayName("against the attack they were written for")
  class AgainstTheAttackTheyWereWrittenFor {

    @Test
    void refuse_the_email_that_worked_in_lesson_one() {
      assertThat(Sanitiser.looksLikeAnInjection(Scenario.INJECTED)).isTrue();
    }

    @Test
    void take_the_card_number_out_of_the_mail_that_carried_it() {
      String cleaned = Sanitiser.redactCards(Scenario.GENUINE);

      assertThat(cleaned).doesNotContain("4111111111114821").contains("[card redacted]");
    }

    @Test
    void leave_the_rest_of_a_genuine_email_alone() {
      assertThat(Sanitiser.redactCards(Scenario.GENUINE)).contains("refund the duplicate charge");
    }
  }

  @Nested
  @DisplayName("against an attacker who has seen them")
  class AgainstAnAttackerWhoHasSeenThem {

    /**
     * The same demand, no listed phrase. Nothing about this email is detectable as an attack,
     * because the sentence that makes it one is the sentence that would make it legitimate.
     */
    @Test
    void do_not_notice_the_same_demand_worded_differently() {
      assertThat(Sanitiser.looksLikeAnInjection(Scenario.INJECTED_POLITELY)).isFalse();
      assertThat(Scenario.INJECTED_POLITELY).contains("999.00");
    }

    /** Four groups of four, as printed on every card in the world. */
    @Test
    void do_not_see_a_card_number_written_the_way_cards_are_written() {
      String cleaned = Sanitiser.redactCards(Scenario.GENUINE_SPACED_CARD);

      assertThat(cleaned)
          .as("the cardholder data this rule exists to protect")
          .contains("4111 1111 1111 4821")
          .doesNotContain("[card redacted]");
    }

    /**
     * The failure nobody sees.
     *
     * <p>Neither of the two above raises anything. A rephrased demand looks like ordinary mail and
     * a spaced card number looks like ordinary text, so a system built on these rules reports
     * nothing unusual on the day it is beaten. The metric a team would watch -- refusals -- goes
     * down.
     */
    @Test
    void report_nothing_at_all_when_they_fail() {
      assertThat(Sanitiser.looksLikeAnInjection(Scenario.INJECTED_POLITELY)).isFalse();
      assertThat(Sanitiser.looksLikeAnInjection(Scenario.GENUINE_SPACED_CARD)).isFalse();
    }
  }
}
