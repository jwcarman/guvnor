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

import org.jwcarman.nessy.api.AgentEvent;
import org.jwcarman.nessy.api.AgentEventListener;
import org.jwcarman.nessy.api.AgentId;
import org.jwcarman.nessy.api.AgentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Says out loud what the agent is doing, so the lesson can be watched rather than inferred.
 *
 * <p>Without this the console shows a started application and then silence, and "nothing happened"
 * is indistinguishable from "something happened and nobody mentioned it". The line worth waiting
 * for is {@code wants to: [issue_credit]} arriving directly after an email that asked for exactly
 * that.
 */
@Component
public class WatchTheDesk implements AgentEventListener {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  @Override
  public void on(AgentType type, AgentId id, AgentEvent event) {
    switch (event) {
      case AgentEvent.TurnStarted started ->
          LOG.info("--> reading an email\n{}", indented(started.observation()));
      case AgentEvent.Thinking ignored -> LOG.info("    thinking");
      case AgentEvent.Commentary said -> LOG.info("    says: {}", said.text());
      case AgentEvent.ActionsRequested actions -> LOG.info("*** wants to: {}", actions.toolNames());
      case AgentEvent.CallFinished done -> LOG.info("    done: {}", done.callId());
      case AgentEvent.CallFailed failed ->
          LOG.info("    failed: {} -- {}", failed.callId(), failed.message());
      case AgentEvent.CallDenied denied ->
          LOG.info("    denied: {} -- {}", denied.callId(), denied.reason());
      case AgentEvent.Answered answered -> LOG.info("<-- answers: {}", answered.text());
      case AgentEvent.TurnFailed ignored -> LOG.warn("<-- the turn failed");
      case AgentEvent.TurnRefused ignored -> LOG.warn("<-- the model refused to answer");
      default -> {
        // The rest are deltas and lifecycle noise; this is a lesson, not a debugger.
      }
    }
  }

  private static String indented(String text) {
    return text.lines().map(line -> "    | " + line).reduce((a, b) -> a + "\n" + b).orElse("");
  }
}
