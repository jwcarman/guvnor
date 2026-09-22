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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.jwcarman.guvnor.domain.billing.AccountId;

/** The desk's correspondence: what arrived, and -- separately -- what it said. */
public class MessageService {

  private final Map<MessageId, Message> messages = new ConcurrentHashMap<>();
  private final Map<MessageId, String> bodies = new ConcurrentHashMap<>();
  private final Consumer<MessageReceived> announce;

  /**
   * @param announce told whenever mail arrives, after it is stored. A {@link Consumer} rather than
   *     anything framework-shaped, so this package stays ordinary Java; an application supplies
   *     whatever its own event machinery needs.
   */
  public MessageService(Consumer<MessageReceived> announce) {
    this.announce = announce;
  }

  /** A desk nobody is listening to. What lesson 0 is, and what every desk was until recently. */
  public MessageService() {
    this(received -> {});
  }

  public Message receive(AccountId from, String subject, String body) {
    Message message = new Message(MessageId.next(), from, subject, Instant.now());
    messages.put(message.id(), message);
    bodies.put(message.id(), body);
    // Stored first, announced second: whoever is listening can read it.
    announce.accept(new MessageReceived(message.id()));
    return message;
  }

  /** Everything that has arrived, oldest first. Metadata only; no body is read to build this. */
  public List<Message> inbox() {
    return messages.values().stream().sorted(Comparator.comparing(Message::receivedAt)).toList();
  }

  public Optional<Message> find(MessageId id) {
    return Optional.ofNullable(messages.get(id));
  }

  /**
   * What the customer wrote.
   *
   * <p>The one call in the desk that hands out text from outside it. Everything a later lesson does
   * to protect this system happens at this line or downstream of it, which is why it is a method
   * rather than a field.
   */
  public Optional<String> body(MessageId id) {
    return Optional.ofNullable(bodies.get(id));
  }
}
