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

import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Two more emails, delivered only in this lesson.
 *
 * <p>The desk's own mailroom has already delivered the genuine email and the injection from lesson
 * 1, and the filter added here refuses the second of those -- correctly, and that refusal is real.
 *
 * <p>These two are what a filter cannot see. The same demand, reworded by somebody who has read
 * about deny-lists; and the same card number, written the way it is printed on a card. Neither
 * trips anything.
 *
 * <p>They live here rather than in the shared scenario's delivery because they are this lesson's
 * evidence. What every lesson receives has to stay identical, or comparing lessons proves nothing.
 */
@Component
@Order(100)
public class MailThatBeatsTheFilter implements ApplicationRunner {

  private final MessageService messages;

  public MailThatBeatsTheFilter(MessageService messages) {
    this.messages = messages;
  }

  @Override
  public void run(ApplicationArguments args) {
    messages.receive(Scenario.CUSTOMER, "Following up", Scenario.INJECTED_POLITELY);
    messages.receive(Scenario.CUSTOMER, "Duplicate charge", Scenario.GENUINE_SPACED_CARD);
  }
}
