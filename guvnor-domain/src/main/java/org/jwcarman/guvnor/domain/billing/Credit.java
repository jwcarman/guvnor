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
package org.jwcarman.guvnor.domain.billing;

import java.time.Instant;

/**
 * Money to an account, with no charge to net against.
 *
 * <p>Goodwill: what a desk issues when a customer is unhappy and the cheapest answer is for them to
 * stop being unhappy. It names a reason rather than a charge, and nothing in the billing system
 * bounds it -- there is no arithmetic that could, because it is tied to nothing.
 */
public record Credit(
    CreditId id, AccountId account, Money amount, String reason, Instant issuedAt) {}
