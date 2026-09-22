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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.UUID;
import org.jwcarman.guvnor.domain.billing.ChargeId;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.billing.Refund;
import org.jwcarman.guvnor.domain.billing.RefundRefused;
import org.jwcarman.guvnor.domain.billing.RefundService;
import org.jwcarman.nessy.api.Awaited;
import org.jwcarman.nessy.api.block.Block;
import org.jwcarman.nessy.api.tool.Tool;
import org.jwcarman.nessy.api.tool.ToolCallRequest;
import org.jwcarman.nessy.api.tool.ToolName;
import org.jwcarman.nessy.api.tool.ToolResult;

/** Refund a charge. Bounded by the charge, so the domain can defend this one on its own. */
public final class RefundTool implements Tool<RefundTool.Input> {

  public record Input(
      @JsonPropertyDescription("The id of the charge to refund") String charge,
      @JsonPropertyDescription("How much to refund, in pence") long pence) {}

  private final RefundService refunds;

  public RefundTool(RefundService refunds) {
    this.refunds = refunds;
  }

  @Override
  public Class<Input> inputType() {
    return Input.class;
  }

  @Override
  public ToolName name() {
    return new ToolName("refund");
  }

  @Override
  public String description() {
    return "Refunds a charge, in whole or in part. Cannot exceed the charge.";
  }

  @Override
  public Awaited<ToolResult> call(ToolCallRequest<Input> request) {
    Input input = request.input();
    try {
      Refund refund =
          refunds.issue(new ChargeId(UUID.fromString(input.charge())), Money.gbp(input.pence()));
      return Awaited.ready(ToolResult.ok(new Block.Text("Refunded " + refund.amount())));
    } catch (RefundRefused refused) {
      return Awaited.ready(new ToolResult.Failure(refused.getMessage()));
    } catch (IllegalArgumentException malformed) {
      return Awaited.ready(new ToolResult.Failure("not a charge id: " + input.charge()));
    }
  }
}
