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
package org.jwcarman.guvnor.domain;

import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.RefundService;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.desk.DeskController;
import org.jwcarman.guvnor.domain.desk.Mailroom;
import org.jwcarman.guvnor.domain.disputes.DisputeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The desk, assembled.
 *
 * <p>Imported by name rather than auto-configured, and that is a choice worth explaining in a
 * series about things being inspectable. An auto-configuration would arrive because a jar is on the
 * classpath and a file in META-INF says so; a reader opening a lesson's application class would
 * find no mention of where the desk came from. One {@code @Import} is the line their eye lands on
 * first.
 *
 * <p>Declared rather than component-scanned for a plainer reason: an application that widens its
 * own scan to cover this package also replaces the filter Spring Boot installs to keep
 * {@code @TestConfiguration} out of the application context, and then every test's private fixtures
 * load into every other test's context.
 *
 * <p>Every service is conditional, so a lesson that needs to put something in the way of one of
 * them -- which is what lesson 3 does to reading a message body -- declares its own and this stands
 * aside.
 */
@Configuration(proxyBeanMethods = false)
public class DomainConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public LedgerService ledgerService() {
    return new LedgerService();
  }

  @Bean
  @ConditionalOnMissingBean
  public ChargeService chargeService() {
    return new ChargeService();
  }

  @Bean
  @ConditionalOnMissingBean
  public RefundService refundService(ChargeService charges, LedgerService ledger) {
    return new RefundService(charges, ledger);
  }

  @Bean
  @ConditionalOnMissingBean
  public CreditService creditService(LedgerService ledger) {
    return new CreditService(ledger);
  }

  /**
   * The mailroom, wired to the application's own events.
   *
   * <p>This is the seam the whole series turns on. The desk receives mail and says so; what happens
   * next is not its business. Lesson 0 has nothing listening, which is exactly why nothing happens
   * in it. Every later lesson listens, and the difference between those lessons is entirely in what
   * their listener does before it reaches a model.
   */
  @Bean
  @ConditionalOnMissingBean
  public MessageService messageService(ApplicationEventPublisher events) {
    return new MessageService(events::publishEvent);
  }

  @Bean
  @ConditionalOnMissingBean
  public DisputeService disputeService() {
    return new DisputeService();
  }

  /** The two scenario emails, delivered at startup to every lesson alike. */
  @Bean
  @ConditionalOnMissingBean
  public Mailroom mailroom(
      ChargeService charges, MessageService messages, DisputeService disputes) {
    return new Mailroom(charges, messages, disputes);
  }

  /** The front of the desk. Declared, like everything else here, rather than scanned for. */
  @Bean
  @ConditionalOnMissingBean
  public DeskController deskController(
      MessageService messages,
      DisputeService disputes,
      LedgerService ledger,
      @Value("${guvnor.lesson:}") String lesson) {
    return new DeskController(messages, disputes, ledger, lesson);
  }
}
