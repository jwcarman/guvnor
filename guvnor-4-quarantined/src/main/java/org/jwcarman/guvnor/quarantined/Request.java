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

/**
 * What a customer appears to be asking for, as a shape rather than as prose.
 *
 * <p>Every field is narrow on purpose. {@link Kind} is an enum, so it can carry four answers and
 * nothing else. The amount is text only because a model writes amounts as text; it is parsed into
 * {@code Money} before anything sees it, and anything that is not an amount is refused there.
 *
 * <p>There is deliberately no free-text field. A {@code String} carries whatever the document put
 * in it, injection and all, so a shape with a "notes" or "reason" on it is a shape with a channel
 * on it -- and the point of this shape is that a model filling it in has nothing to say.
 */
public record Request(Kind kind, String amount) {

  public enum Kind {
    /** Money back against a charge the customer says they should not have paid. */
    REFUND,
    /** Money for the trouble, with no charge behind it. */
    GOODWILL_CREDIT,
    /** Something else, or nothing this desk can act on. */
    OTHER
  }
}
