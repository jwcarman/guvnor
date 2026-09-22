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

  public enum Sensitivity {
    ORDINARY,
    PERSONAL,
    CARDHOLDER
  }

  public static final Axis<Sensitivity> SENSITIVITY =
      Axis.ladder(
          "sensitivity", Sensitivity.ORDINARY, Sensitivity.PERSONAL, Sensitivity.CARDHOLDER);

  public static Axes all() {
    return Axes.of(SENSITIVITY);
  }
}
