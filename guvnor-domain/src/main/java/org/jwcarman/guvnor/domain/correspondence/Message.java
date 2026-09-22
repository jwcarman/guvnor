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
package org.jwcarman.guvnor.domain.correspondence;

import java.time.Instant;
import org.jwcarman.guvnor.domain.billing.AccountId;

/**
 * What arrived, and nothing of what it said.
 *
 * <p>There is no body on this record, deliberately. Mail and ticketing systems keep the body out of
 * the record and fetch it when somebody actually reads it, because the body is large, it is rarely
 * needed, and it is the part that came from outside. Modelling that accurately means the desk can
 * list, sort, assign and report on its correspondence without any of it ever handling what a
 * customer wrote.
 *
 * <p>Reading a body is therefore a separate, visible act -- {@link MessageService#body} -- rather
 * than a field access nobody notices. Later lessons put something in the way of that act. Nothing
 * about this record has to change when they do.
 */
public record Message(MessageId id, AccountId from, String subject, Instant receivedAt) {}
