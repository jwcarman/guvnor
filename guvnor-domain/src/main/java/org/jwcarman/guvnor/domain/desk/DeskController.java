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
package org.jwcarman.guvnor.domain.desk;

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
 * The front of the desk, and it is the same front in every lesson.
 *
 * <p>Mail arrives, it is stored, a case is opened, and the desk says so. That is the whole of it.
 * There is no agent here, no filter, no policy and no model, because none of those are the desk's
 * business -- they are what different applications do with the announcement.
 *
 * <p>Keeping this identical across the series is what makes the series legible. A reader comparing
 * two lessons is not comparing two web applications that happen to differ; they are comparing what
 * each one does when the same desk says the same thing.
 */
@Controller
public class DeskController {

  private final MessageService messages;
  private final DisputeService disputes;
  private final LedgerService ledger;

  /** A line of prose naming which lesson this is, so a reader knows what they are looking at. */
  private final String lesson;

  public DeskController(
      MessageService messages, DisputeService disputes, LedgerService ledger, String lesson) {
    this.messages = messages;
    this.disputes = disputes;
    this.ledger = ledger;
    this.lesson = lesson;
  }

  @GetMapping("/")
  public String inbox(Model model) {
    model.addAttribute("messages", messages.inbox());
    model.addAttribute("entries", ledger.entries());
    model.addAttribute("customer", Scenario.CUSTOMER);
    model.addAttribute("credited", ledger.creditedTo(Scenario.CUSTOMER, Money.usd(0L)));
    model.addAttribute(
        "everything",
        ledger.entries().stream().map(entry -> entry.amount()).reduce(Money.usd(0L), Money::plus));
    model.addAttribute("lesson", lesson);
    return "desk";
  }

  /**
   * An email arrives.
   *
   * <p>Post anything here, including instructions addressed to the software. What happens next is
   * not decided in this method -- it is decided by whatever is listening, which is the only thing
   * that changes from one lesson to the next.
   */
  @PostMapping("/mail")
  public String receive(
      @RequestParam String subject, @RequestParam String body, RedirectAttributes flash) {
    Message arrived = messages.receive(Scenario.CUSTOMER, subject, body);
    disputes.open(arrived.id());
    flash.addFlashAttribute("said", "Delivered.");
    return "redirect:/";
  }
}
