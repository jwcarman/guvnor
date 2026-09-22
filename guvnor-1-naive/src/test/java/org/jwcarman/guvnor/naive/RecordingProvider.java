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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.jwcarman.nessy.api.block.Block;
import org.jwcarman.nessy.api.turn.Turn;
import org.jwcarman.nessy.spi.inference.InferenceProvider;
import org.jwcarman.nessy.spi.inference.InferenceRequest;
import org.jwcarman.nessy.spi.inference.InferenceResult;
import org.jwcarman.nessy.spi.narration.AgentNarrator;

/**
 * A probe, not a model.
 *
 * <p>It makes no decisions and calls no tools. It writes down what it was handed and answers with a
 * full stop, which is the least a provider can do and still let a turn finish.
 *
 * <p>This is deliberately not a stand-in that falls for the injected email. A fake that I
 * programmed to issue a credit would prove that I can write a regular expression, and nothing else.
 * What this lesson's tests assert instead is what the agent was *given* -- which is deterministic,
 * needs no model, and is the actual defect. Whether a real model then acts on it is a question
 * about real models, and it is answered by the transcript in the README.
 */
public final class RecordingProvider implements InferenceProvider {

  private final AtomicReference<String> lastPrompt = new AtomicReference<>();

  @Override
  public InferenceResult infer(InferenceRequest request, AgentNarrator narrator) {
    StringBuilder everything = new StringBuilder();
    for (Turn turn : request.context().turns()) {
      everything.append(turn.observation()).append('\n');
    }
    lastPrompt.set(everything.toString());
    return new InferenceResult.Answer(List.of(new Block.Text("Noted.")));
  }

  /** Everything the agent was handed, as one flat piece of text -- because that is what it is. */
  public String lastPrompt() {
    return lastPrompt.get();
  }
}
