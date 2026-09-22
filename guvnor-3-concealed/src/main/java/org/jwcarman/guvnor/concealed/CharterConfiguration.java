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

import static org.jwcarman.guvnor.concealed.Vocabulary.SENSITIVITY;
import static org.jwcarman.guvnor.concealed.Vocabulary.Sensitivity.CARDHOLDER;
import static org.jwcarman.guvnor.concealed.Vocabulary.Sensitivity.PERSONAL;

import java.util.regex.Pattern;
import org.jwcarman.loch.Charter;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.MemoryStorage;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Storage;
import org.jwcarman.loch.lattice.Axes;
import org.jwcarman.loch.lattice.Ceiling;
import org.jwcarman.loch.lattice.Constraint;
import org.jwcarman.loch.lattice.Label;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Everything this application is allowed to do with a customer's mail, declared in one place.
 *
 * <p>This file is the lesson. It is not a filter and it is not advice: it is the set of doors that
 * exist, and there is no other way in or out. Code that wants to do something not declared here
 * does not do it badly, it does not compile or it is refused at the door.
 */
@Configuration(proxyBeanMethods = false)
public class CharterConfiguration {

  /** Card numbers, spaced or not, because both are how people write them. */
  private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

  @Bean
  public Axes deskAxes() {
    return Vocabulary.all();
  }

  /** In memory, because this is a lesson. A real desk hands the starter a JDBC storage. */
  @Bean
  public Storage deskStorage() {
    return new MemoryStorage();
  }

  /**
   * How mail gets in: as cardholder data, always.
   *
   * <p>Not "if it looks like it contains a card". Mail to a billing desk may contain a card number,
   * so all of it is labelled as though it does. That is the difference between a guess about this
   * message and a fact about this channel.
   */
  @Bean
  public Conceal<String> inboundMail(Charter charter) {
    return charter.source("customer-mail", Mail.TYPE, ctx -> Label.of(SENSITIVITY, CARDHOLDER));
  }

  /**
   * How mail gets out to a model: only if it is no more sensitive than personal.
   *
   * <p>The ceiling is the whole protection. A model is a third party that keeps what it is shown,
   * so this door does not admit cardholder data -- and "does not admit" means the read is refused,
   * not that somebody remembered to sanitise first.
   */
  @Bean
  public Reveal<String> supportModel(Charter charter) {
    return charter
        .destination(
            "support-model", ctx -> Ceiling.of(SENSITIVITY, Constraint.atMost(PERSONAL)), Mail.TYPE)
        .reading(Mail.TYPE);
  }

  /**
   * The one thing in this application that can make mail less sensitive.
   *
   * <p>A derivation, named, declared here, and the only route from CARDHOLDER to PERSONAL. It
   * appears in the charter's manifest under "can WEAKEN a label", which is the list an auditor asks
   * for by name.
   *
   * <p>The redaction inside it is a regular expression, and it is no cleverer than the one lesson 2
   * used. What changed is not the quality of the rule but its position: there is exactly one of it,
   * it is discoverable, and every other path from a customer's mail to a model is refused rather
   * than merely unwise.
   */
  @Bean
  public Derivation<String, String> redactedMail(Charter charter) {
    return charter.derivation(
        "mail.redacted",
        Mail.TYPE,
        Mail.TYPE,
        text -> CARD.matcher(text).replaceAll("[card redacted]"),
        d ->
            d.accepting(ctx -> Ceiling.of(SENSITIVITY, Constraint.any()))
                .lowering(joined -> joined.with(SENSITIVITY, PERSONAL)));
  }
}
