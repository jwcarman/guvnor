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

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.UUID;
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Credit;
import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.loch.Conceal;
import org.jwcarman.loch.Derivation;
import org.jwcarman.loch.Derived;
import org.jwcarman.loch.Reveal;
import org.jwcarman.loch.Revealed;
import org.jwcarman.loch.Surrogate;
import org.jwcarman.nessy.api.Awaited;
import org.jwcarman.nessy.api.block.Block;
import org.jwcarman.nessy.api.tool.Tool;
import org.jwcarman.nessy.api.tool.ToolCallRequest;
import org.jwcarman.nessy.api.tool.ToolName;
import org.jwcarman.nessy.api.tool.ToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Propose a goodwill credit.
 *
 * <p>The name of the class still says "issue" and the model is still told it can issue one,
 * deliberately: nothing about the agent's view of the world has been made more honest. What changed
 * is underneath.
 *
 * <p>Whatever the model asks for is concealed as an UNENDORSED claim, and the authority that moves
 * money admits only ENDORSED. So this method cannot issue anything by being persuasive. It can only
 * propose, and then find out whether the billing system agrees.
 */
public final class IssueCreditTool implements Tool<IssueCreditTool.Input> {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  public record Input(
      @JsonPropertyDescription("The account to credit") String account,
      @JsonPropertyDescription("How much to credit, in dollars, like 42.00") Money amount) {}

  private final CreditService credits;
  private final Conceal<Claim> proposed;
  private final Derivation<Claim, Claim> confirmed;
  private final Reveal<Claim> authority;

  public IssueCreditTool(
      CreditService credits,
      Conceal<Claim> proposed,
      Derivation<Claim, Claim> confirmed,
      Reveal<Claim> authority) {
    this.credits = credits;
    this.proposed = proposed;
    this.confirmed = confirmed;
    this.authority = authority;
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
    return "Issues a goodwill credit to an account.";
  }

  @Override
  public Awaited<ToolResult> call(ToolCallRequest<Input> request) {
    Input input = request.input();
    if (input.amount() == null) {
      return Awaited.ready(new ToolResult.Failure("no amount was given; say it like 42.00"));
    }

    // What the model wants, as a claim nobody has agreed with yet.
    Surrogate<Claim> claimed =
        proposed.conceal(
            Claim.proposed(new AccountId(UUID.fromString(input.account())), input.amount()));

    // The only route to ENDORSED, and the model has no say in how it answers.
    Derived<Claim> endorsed = confirmed.derive(claimed);
    if (!(endorsed instanceof Derived.Made<Claim>(Surrogate<Claim> supported))) {
      LOG.info("the billing system does not support this claim, so it stays unendorsed");
      return Awaited.ready(
          new ToolResult.Failure(
              "This account has no charge that supports a credit of "
                  + input.amount()
                  + ". A goodwill credit has to be about something."));
    }

    // Even now the authority is asked rather than assumed.
    Revealed<Claim> permitted = authority.reveal(supported);
    if (permitted instanceof Revealed.Denied<Claim>(var reason, var detail)) {
      LOG.info("the credit authority refused: {} -- {}", reason, detail);
      return Awaited.ready(new ToolResult.Failure("That credit is not permitted: " + detail));
    }

    Claim claim = ((Revealed.Allowed<Claim>) permitted).value();
    Credit credit = credits.issue(claim.account(), claim.amount(), claim.basis());
    return Awaited.ready(ToolResult.ok(new Block.Text("Credited " + credit.amount())));
  }
}
