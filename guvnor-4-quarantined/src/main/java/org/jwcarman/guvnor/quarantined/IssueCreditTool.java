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
import org.jwcarman.guvnor.domain.billing.Credit;
import org.jwcarman.guvnor.domain.billing.CreditService;
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
 * Act on a claim.
 *
 * <p>Note what this tool does not take: an amount. The agent cannot say how much, because the
 * amount is not the agent's to say -- it belongs to the claim the quarantine produced, and the
 * agent is holding a reference to that claim rather than a copy of its contents.
 *
 * <p>An earlier version took an account and an amount, and the agent retyped both from a
 * description it had been given. That is a channel: a model told "the customer wants $42.00" can
 * call the tool with $5,000.00, and nothing in the arrangement would have noticed, because the
 * arrangement never saw the claim the agent was supposedly acting on.
 *
 * <p>Now the worst a persuaded agent can do is ask for a claim that exists to be acted on. Whether
 * it may be is still decided by the billing system.
 */
public final class IssueCreditTool implements Tool<IssueCreditTool.Input> {

  private static final Logger LOG = LoggerFactory.getLogger("desk");

  public record Input(@JsonPropertyDescription("The id of the claim to act on") String claim) {}

  private final CreditService credits;
  private final Derivation<Claim, Claim> confirmed;
  private final Reveal<Claim> authority;

  public IssueCreditTool(
      CreditService credits, Derivation<Claim, Claim> confirmed, Reveal<Claim> authority) {
    this.credits = credits;
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
    return "Issues the goodwill credit a claim asks for, if the billing system supports it.";
  }

  @Override
  public Awaited<ToolResult> call(ToolCallRequest<Input> request) {
    if (request.input().claim() == null || request.input().claim().isBlank()) {
      return Awaited.ready(new ToolResult.Failure("no claim was named"));
    }

    // A reference to something that already exists. Nothing is authored here.
    Surrogate<Claim> claimed = Surrogate.of(request.input().claim());

    // The only route to ENDORSED, and no model has a say in how it answers.
    Derived<Claim> endorsed = confirmed.derive(claimed);
    if (!(endorsed instanceof Derived.Made<Claim>(Surrogate<Claim> supported))) {
      LOG.info("the billing system does not support this claim, so it stays unendorsed");
      return Awaited.ready(
          new ToolResult.Failure(
              "The billing system has no charge that supports this claim. A goodwill credit has"
                  + " to be about something."));
    }

    // Even now the authority is asked rather than assumed.
    Revealed<Claim> permitted = authority.reveal(supported);
    if (permitted instanceof Revealed.Denied<Claim>(var reason, var detail)) {
      LOG.info("the credit authority refused: {} -- {}", reason, detail);
      return Awaited.ready(new ToolResult.Failure("That credit is not permitted: " + detail));
    }

    Claim claim = ((Revealed.Allowed<Claim>) permitted).value();

    // A check, and it should not have to be. What a claim is FOR belongs on its label, where a
    // door could refuse it -- but a derived value's label is computed by lowering(), which is
    // handed the incoming label and not the value produced, so "this claim asks for a credit"
    // cannot be said in the lattice today. Until it can, this is an if-statement doing a door's
    // job, and it is exactly the kind of thing this lesson argues against.
    if (claim.kind() != Request.Kind.GOODWILL_CREDIT) {
      LOG.info("that claim asks for {}, which is not this authority's business", claim.kind());
      return Awaited.ready(
          new ToolResult.Failure("That claim is not asking for a goodwill credit."));
    }
    // The ledger's reason is built here, from a charge id, rather than carried as text through
    // anything a model touched.
    Credit credit =
        credits.issue(
            claim.account(), claim.amount(), "supported by charge " + claim.supportedBy());
    return Awaited.ready(ToolResult.ok(new Block.Text("Credited " + credit.amount())));
  }
}
