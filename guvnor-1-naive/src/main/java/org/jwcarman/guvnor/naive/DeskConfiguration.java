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

import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.RefundService;
import org.jwcarman.nessy.api.Harness;
import org.jwcarman.nessy.engine.harness.DefaultHarnessFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class DeskConfiguration {

  /**
   * The agent, holding both authorities.
   *
   * <p>No approver on either tool, no filter on the mail, no check on what comes back. That is not
   * an oversight in this lesson; it is the lesson.
   */
  @Bean
  public Harness<String> harness(
      DefaultHarnessFactory factory, RefundService refunds, CreditService credits) {
    return factory.create(
        String.class,
        config ->
            config
                .agentType(Desk.TYPE)
                .systemPrompt(Desk.SYSTEM_PROMPT)
                .tool(new RefundTool(refunds))
                .tool(new IssueCreditTool(credits)));
  }
}
