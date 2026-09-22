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

import java.util.List;
import java.util.regex.Pattern;

/**
 * The defences people actually ship.
 *
 * <p>Nothing here is a straw man. Redacting card numbers with a regular expression and refusing
 * mail that contains known injection phrasing is what a careful team writes on the Monday after
 * lesson 1 goes wrong, and it is what most production systems handling untrusted text are doing
 * today.
 *
 * <p>It is also the last module in this series that tries to solve the problem by looking at the
 * text, and the reason is in the tests: both rules work perfectly on the attack they were written
 * for, and neither one survives an attacker who has seen them. The defence and the thing defended
 * against are the same kind of thing -- prose, judged by pattern -- so the contest is open-ended,
 * and the attacker moves last.
 */
public final class Sanitiser {

  /** Thirteen to nineteen digits, which is what a card number is. */
  private static final Pattern CARD = Pattern.compile("\\b\\d{13,19}\\b");

  /**
   * The phrasings from the incident.
   *
   * <p>Every deny-list in the world starts as the list of things that already happened.
   */
  private static final List<String> KNOWN_INJECTIONS =
      List.of(
          "ignore previous instructions",
          "ignore all previous",
          "you are authorised",
          "you are authorized",
          "disregard the above",
          "system override");

  private Sanitiser() {}

  /** Replaces anything card-shaped, so cardholder data cannot reach a third party. */
  public static String redactCards(String text) {
    return CARD.matcher(text).replaceAll("[card redacted]");
  }

  /** True when the mail contains phrasing we have seen used to hijack the desk. */
  public static boolean looksLikeAnInjection(String text) {
    String lowered = text.toLowerCase(java.util.Locale.ROOT);
    return KNOWN_INJECTIONS.stream().anyMatch(lowered::contains);
  }
}
