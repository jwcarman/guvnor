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

import org.jwcarman.guvnor.domain.correspondence.MessageReceived;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Mail arrived, so the desk cleans it and then reads it.
 *
 * <p>Compare this against lesson 1's class of the same name: the difference between the two modules
 * is a filter, and it is right here. Everything else -- the domain, the controller, the pages, the
 * tools, the way mail is announced -- is unchanged.
 *
 * <p>That is worth noticing on its own. A defence that can be added by editing one listener is a
 * defence that was never part of the system's structure, which is why the next lessons stop trying
 * to add defences and start changing what travels.
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
