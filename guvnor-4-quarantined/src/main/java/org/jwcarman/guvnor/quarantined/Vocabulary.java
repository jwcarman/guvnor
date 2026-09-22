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

import org.jwcarman.loch.lattice.Axes;
import org.jwcarman.loch.lattice.Axis;

/**
 * What this desk asks about every value it holds.
 *
 * <p>One question, for now: how sensitive is this? A ladder, so the answers are ordered and
 * "ordinary" is below "personal" is below "cardholder". A later lesson adds a second axis, when the
 * path hits the wall that requires one.
 */
public final class Vocabulary {

  private Vocabulary() {}

  /**
   * How much this is to be believed.
   *
   * <p>ENDORSED first, so it is the BOTTOM of the ladder and UNENDORSED is above it. That looks
   * backwards until you say what a ceiling means: a door admitting "at most ENDORSED" admits only
   * what has been endorsed, and anything less trusted is above it and refused. Untrusted is the
   * more constrained end, which is Biba's integrity model and the opposite of how a sensitivity
   * ladder reads.
   */
  public enum Integrity {
    ENDORSED,
    UNENDORSED
  }

  public enum Sensitivity {
    ORDINARY,
    PERSONAL,
    CARDHOLDER
  }

  public static final Axis<Sensitivity> SENSITIVITY =
      Axis.ladder(
          "sensitivity", Sensitivity.ORDINARY, Sensitivity.PERSONAL, Sensitivity.CARDHOLDER);

  public static final Axis<Integrity> INTEGRITY =
      Axis.ladder("integrity", Integrity.ENDORSED, Integrity.UNENDORSED);

  /**
   * What the value is a claim about.
   *
   * <p>A matching axis rather than a ladder: refund and goodwill credit are not more or less than
   * each other, they are different, and a door that moves goodwill money has no business reading a
   * claim about a refund.
   *
   * <p>This is here because an agent used the wrong one. The quarantine read REFUND of $42.00, the
   * agent called the credit tool, and the endorsement agreed -- correctly, because a $42.00 claim
   * really is supported by a $42.00 charge. The kind was being carried and not enforced, which is a
   * distinction with no difference until the day it has one.
   */
  public static final Axis<String> ASKS = Axis.matching("asks").required();

  public static Axes all() {
    return Axes.of(SENSITIVITY, INTEGRITY, ASKS);
  }
}
