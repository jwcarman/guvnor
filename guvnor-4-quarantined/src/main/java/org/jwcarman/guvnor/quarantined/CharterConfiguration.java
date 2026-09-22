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

import static org.jwcarman.guvnor.quarantined.Vocabulary.INTEGRITY;
import static org.jwcarman.guvnor.quarantined.Vocabulary.Integrity.ENDORSED;
import static org.jwcarman.guvnor.quarantined.Vocabulary.Integrity.UNENDORSED;
import static org.jwcarman.guvnor.quarantined.Vocabulary.SENSITIVITY;
import static org.jwcarman.guvnor.quarantined.Vocabulary.Sensitivity.CARDHOLDER;
import static org.jwcarman.guvnor.quarantined.Vocabulary.Sensitivity.ORDINARY;
import static org.jwcarman.guvnor.quarantined.Vocabulary.Sensitivity.PERSONAL;

import java.util.Optional;
import java.util.regex.Pattern;
import org.jwcarman.guvnor.domain.billing.ChargeService;
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
    return charter.source(
        "customer-mail",
        Mail.TYPE,
        ctx -> Label.of(SENSITIVITY, CARDHOLDER).with(INTEGRITY, UNENDORSED));
  }

  /**
   * How mail gets out to a model: only if it is no more sensitive than personal.
   *
   * <p>The ceiling is the whole protection. A model is a third party that keeps what it is shown,
   * so this door does not admit cardholder data -- and "does not admit" means the read is refused,
   * not that somebody remembered to sanitise first.
   *
   * <p>Note what it says about integrity: anything. The model is allowed to read untrusted text,
   * because reading untrusted text is its entire job. Quarantine is not about keeping the model
   * away from the customer's words. It is about what the model's output is then worth.
   */
  @Bean
  public Reveal<String> supportModel(Charter charter) {
    return charter
        .destination(
            "support-model",
            ctx ->
                Ceiling.of(SENSITIVITY, Constraint.atMost(PERSONAL))
                    .with(INTEGRITY, Constraint.any()),
            Mail.TYPE)
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
            d.accepting(
                    ctx ->
                        Ceiling.of(SENSITIVITY, Constraint.any()).with(INTEGRITY, Constraint.any()))
                .lowering(joined -> joined.with(SENSITIVITY, PERSONAL)));
  }

  /**
   * The quarantined read, as a declassification the lattice can see.
   *
   * <p>This was a {@code source} until somebody noticed what that meant. Reading the mail,
   * extracting a claim and concealing the result minted a value with an asserted label -- ORDINARY,
   * from mail that was CARDHOLDER -- which is a lowering, performed outside the system that exists
   * to govern lowerings. It did not appear in the manifest under "can WEAKEN a label", and the
   * lineage had no edge from the mail to the claim, so "where did this come from" ended at a source
   * that claimed to know.
   *
   * <p>As a derivation it is all three of the things it should be: the lowering is declared, the
   * provenance is recorded, and a reviewer asking what can declassify gets told about it.
   *
   * <p>Note what it does NOT lower: anything. This derivation changes how structured a value is,
   * and structuredness is not a security property. What made the mail less sensitive was the
   * scrubber, in its own named derivation, which is where a reviewer should look for a
   * declassification -- not buried in an extraction that happens to produce a narrow shape.
   *
   * <p>So a claim comes out carrying exactly the label its mail had after scrubbing: personal, and
   * unendorsed. A model read a stranger's email to produce it, so it is precisely as untrusted as
   * what it came from. A model may transform a value; it may not be the reason a value becomes more
   * trusted, or less sensitive.
   */
  @Bean
  public Derivation<String, Claim> quarantinedRead(Charter charter, Quarantine quarantine) {
    return charter.checking(
        "mail.claim",
        Mail.TYPE,
        Claim.TYPE,
        quarantine::read,
        d ->
            d.accepting(
                    ctx ->
                        Ceiling.of(SENSITIVITY, Constraint.any()).with(INTEGRITY, Constraint.any()))
                .lowering(joined -> joined.with(SENSITIVITY, ORDINARY)));
  }

  /**
   * The authority that moves money, and what it will admit.
   *
   * <p>At most ENDORSED. An unendorsed claim is above that ceiling, so it cannot arrive here -- not
   * "is checked and rejected", cannot arrive. This is the line lessons 1 through 3 did not have,
   * and it is the reason the model's judgement stops being load-bearing.
   */
  @Bean
  public Reveal<Claim> creditAuthority(Charter charter) {
    return charter
        .destination(
            "credit-authority",
            ctx ->
                Ceiling.of(SENSITIVITY, Constraint.atMost(PERSONAL))
                    .with(INTEGRITY, Constraint.atMost(ENDORSED)),
            Claim.TYPE)
        .reading(Claim.TYPE);
  }

  /**
   * The bridge, and the only one.
   *
   * <p>What elevates a claim is agreement with something already trusted. This looks the account up
   * in the billing system and endorses the claim only if a real charge supports it -- and the
   * lookup is the derivation's own, so nothing the model says can reach into it.
   *
   * <p>It appears in the charter's manifest under "can WEAKEN a label". That is the list an auditor
   * asks for, and this is the only entry on it that touches money. One line to review rather than a
   * codebase to trust.
   */
  @Bean
  public Derivation<Claim, Claim> confirmedClaim(Charter charter, ChargeService charges) {
    return charter.checking(
        "claim.confirmed",
        Claim.TYPE,
        Claim.TYPE,
        (claim, ctx) -> supportedByACharge(charges, claim),
        d ->
            d.accepting(
                    ctx ->
                        Ceiling.of(SENSITIVITY, Constraint.atMost(PERSONAL))
                            .with(INTEGRITY, Constraint.any()))
                .lowering(joined -> joined.with(INTEGRITY, ENDORSED)));
  }

  /**
   * Agreement with something already trusted.
   *
   * <p>A goodwill credit is endorsed when the account actually has a charge it could plausibly be
   * about, and the amount does not exceed it. A desk can argue about whether that is the right rule
   * -- it is deliberately a simple one -- but not about where it lives: it is here, it is the only
   * way to ENDORSED, and no email can reach it.
   */
  private static Optional<Claim> supportedByACharge(ChargeService charges, Claim claim) {
    return charges.forAccount(claim.account()).stream()
        .filter(charge -> !claim.amount().isGreaterThan(charge.amount()))
        .findFirst()
        // The endorsement replaces the model's account of why, with the desk's own. What the
        // model wrote does not travel any further than this method.
        .map(charge -> claim.supportedBy(charge.id()));
  }
}
