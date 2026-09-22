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
package org.jwcarman.guvnor.naive;

import org.jwcarman.guvnor.domain.correspondence.MessageReceived;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mail arrived, so the desk reads it.
 *
 * <p>This class is the whole of lesson 1. The desk in lesson 0 announced arriving mail exactly as
 * this one does; the difference is that here something is listening, and what it does is hand the
 * customer's words to a model that holds two authorities for moving money.
 *
 * <p>Every later lesson replaces this class and changes nothing else.
 */
@Component
public class TheDeskListens {

  private final DeskAgent desk;

  public TheDeskListens(DeskAgent desk) {
    this.desk = desk;
  }

  @EventListener
  public void onMessageReceived(MessageReceived received) {
    desk.handle(received.message());
  }
}
