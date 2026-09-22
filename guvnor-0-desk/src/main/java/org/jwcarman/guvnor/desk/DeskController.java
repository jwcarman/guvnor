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
package org.jwcarman.guvnor.desk;

import java.util.List;
import org.jwcarman.guvnor.domain.billing.AccountId;
import org.jwcarman.guvnor.domain.billing.Charge;
import org.jwcarman.guvnor.domain.billing.ChargeId;
import org.jwcarman.guvnor.domain.billing.ChargeService;
import org.jwcarman.guvnor.domain.billing.CreditService;
import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.billing.RefundRefused;
import org.jwcarman.guvnor.domain.billing.RefundService;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageId;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The desk, as an operator sees it.
 *
 * <p>Every authority in here is reached by a person clicking a button. The email is displayed;
 * nothing in this application acts on what it says, because nothing in this application reads it.
 */
@Controller
public class DeskController {

  private final MessageService messages;
  private final ChargeService charges;
  private final RefundService refunds;
  private final CreditService credits;
  private final LedgerService ledger;

  public DeskController(
      MessageService messages,
      ChargeService charges,
      RefundService refunds,
      CreditService credits,
      LedgerService ledger) {
    this.messages = messages;
    this.charges = charges;
    this.refunds = refunds;
    this.credits = credits;
    this.ledger = ledger;
  }

  @GetMapping("/")
  public String inbox(Model model) {
    model.addAttribute("messages", messages.inbox());
    model.addAttribute("entries", ledger.entries());
    model.addAttribute("credited", ledger.creditedTo(Scenario.CUSTOMER, Money.gbp(0L)));
    return "inbox";
  }

  /** The operator opens a message. This is the only thing in the desk that reads a body. */
  @GetMapping("/mail/{id}")
  public String read(@PathVariable String id, Model model) {
    MessageId messageId = new MessageId(java.util.UUID.fromString(id));
    Message message = messages.find(messageId).orElseThrow();
    List<Charge> account = charges.forAccount(message.from());

    model.addAttribute("message", message);
    model.addAttribute("body", messages.body(messageId).orElse(""));
    model.addAttribute("charges", account);
    return "mail";
  }

  @PostMapping("/mail/{id}/refund")
  public String refund(
      @PathVariable String id,
      @RequestParam String charge,
      @RequestParam long pence,
      RedirectAttributes flash) {
    ChargeId chargeId = new ChargeId(java.util.UUID.fromString(charge));
    try {
      refunds.issue(chargeId, Money.gbp(pence));
      flash.addFlashAttribute("said", "Refunded " + Money.gbp(pence));
    } catch (RefundRefused refused) {
      flash.addFlashAttribute("said", "Refused: " + refused.getMessage());
    }
    return "redirect:/mail/" + id;
  }

  @PostMapping("/mail/{id}/credit")
  public String credit(
      @PathVariable String id,
      @RequestParam String account,
      @RequestParam long pence,
      @RequestParam String reason,
      RedirectAttributes flash) {
    AccountId accountId = new AccountId(java.util.UUID.fromString(account));
    credits.issue(accountId, Money.gbp(pence), reason);
    flash.addFlashAttribute("said", "Credited " + Money.gbp(pence));
    return "redirect:/mail/" + id;
  }
}
