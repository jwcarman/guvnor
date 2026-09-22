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

import org.jwcarman.guvnor.domain.billing.LedgerService;
import org.jwcarman.guvnor.domain.billing.Money;
import org.jwcarman.guvnor.domain.correspondence.Message;
import org.jwcarman.guvnor.domain.correspondence.MessageService;
import org.jwcarman.guvnor.domain.disputes.DisputeService;
import org.jwcarman.guvnor.domain.scenario.Scenario;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * The desk, now with nobody at it.
 *
 * <p>The difference from lesson 0 is one line: an email that arrives is handed straight to the
 * agent. No button, no operator, no pause.
 */
@Controller
public class DeskController {

  private final MessageService messages;
  private final DisputeService disputes;
  private final LedgerService ledger;
  private final DeskAgent desk;

  public DeskController(
      MessageService messages, DisputeService disputes, LedgerService ledger, DeskAgent desk) {
    this.messages = messages;
    this.disputes = disputes;
    this.ledger = ledger;
    this.desk = desk;
  }

  @GetMapping("/")
  public String inbox(Model model) {
    model.addAttribute("messages", messages.inbox());
    model.addAttribute("entries", ledger.entries());
    model.addAttribute("credited", ledger.creditedTo(Scenario.CUSTOMER, Money.usd(0L)));
    // Everything that left, whoever it went to. The per-customer figure above answers a
    // different question, and the gap between them is worth seeing: an agent that credits an
    // account nobody wrote in from still moved real money.
    model.addAttribute(
        "everything",
        ledger.entries().stream().map(e -> e.amount()).reduce(Money.usd(0L), Money::plus));
    model.addAttribute("customer", Scenario.CUSTOMER);
    return "inbox";
  }

  /**
   * An email arrives, and the desk gets on with it.
   *
   * <p>This is the whole of lesson 1. Everything else in this module is the same desk as lesson 0.
   */
  @PostMapping("/mail")
  public String receive(
      @RequestParam String subject, @RequestParam String body, RedirectAttributes flash) {
    Message arrived = messages.receive(Scenario.CUSTOMER, subject, body);
    disputes.open(arrived.id());
    desk.handle(arrived.id());
    flash.addFlashAttribute("said", "Delivered, and handed to the desk.");
    return "redirect:/";
  }
}
