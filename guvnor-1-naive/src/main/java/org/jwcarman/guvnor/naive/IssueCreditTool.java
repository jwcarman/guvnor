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
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Credit;
import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.nessy.api.Awaited;
import org.jwcarman.nessy.api.block.Block;
import org.jwcarman.nessy.api.tool.Tool;
import org.jwcarman.nessy.api.tool.ToolCallRequest;
import org.jwcarman.nessy.api.tool.ToolName;
import org.jwcarman.nessy.api.tool.ToolResult;

/**
 * Issue a goodwill credit.
 *
 * <p>This is the dangerous one, and nothing about it says so. It is the same shape as the refund
 * tool, it is registered the same way, and it reads exactly as reasonable in a system prompt. The
 * difference is that no arithmetic anywhere bounds what it hands out.
 */
public final class IssueCreditTool implements Tool<IssueCreditTool.Input> {

  public record Input(
      @JsonPropertyDescription("The account to credit") String account,
      @JsonPropertyDescription("How much to credit, in dollars, like 42.00") Money amount,
      @JsonPropertyDescription("Why this credit is being issued") String reason) {}

  private final CreditService credits;

  public IssueCreditTool(CreditService credits) {
    this.credits = credits;
  }

  @Override
  public Class<Input> inputType() {
    return Input.class;
  }

  @Override
  public ToolName name() {
    return new ToolName("issue_credit");
  }

  @Override
  public String description() {
    return "Issues a goodwill credit to an account, for any amount, for a stated reason.";
  }

  @Override
  public Awaited<ToolResult> call(ToolCallRequest<Input> request) {
    Input input = request.input();
    if (input.amount() == null) {
      return Awaited.ready(new ToolResult.Failure("no amount was given; say it like 42.00"));
    }
    try {
      Credit credit =
          credits.issue(
              new AccountId(UUID.fromString(input.account())), input.amount(), input.reason());
      return Awaited.ready(ToolResult.ok(new Block.Text("Credited " + credit.amount())));
    } catch (IllegalArgumentException malformed) {
      // A failure rather than an exception: the model can read this and try again with a better
      // argument, which is what ToolResult.Failure is for.
      return Awaited.ready(new ToolResult.Failure(malformed.getMessage()));
    }
  }
}
