package org.openbank.controller;

import org.openbank.view.BankViewService;
import org.openbank.service.DepositService;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ManagerDepositsController {

  private static final int PAGE_SIZE = 5;

  private final DepositService depositService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public ManagerDepositsController(DepositService depositService, BankViewService bankViewService, MessageService messageService) {
    this.depositService = depositService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/manager/deposits")
  public String deposits(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
    model.addAttribute("pendingDepositsPage", bankViewService.getPendingDepositViewsPage(page, PAGE_SIZE));
    return "manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/approve")
  public String approve(@PathVariable("depositId") Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.approveDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", messageService.get("manager.deposits.approve.success"));
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", messageService.get("manager.deposits.approve.error"));
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/reject")
  public String reject(@PathVariable("depositId") Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.rejectDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", messageService.get("manager.deposits.reject.success"));
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", messageService.get("manager.deposits.reject.error"));
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/process-expiration")
  public String processExpiration(RedirectAttributes redirectAttributes) {
    int count = depositService.processExpiredDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", messageService.get("manager.deposits.processExpiration.success", count));
    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/accrue-interest")
  public String accrueInterest(RedirectAttributes redirectAttributes) {
    int count = depositService.accrueInterestForActiveDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", messageService.get("manager.deposits.accrueInterest.success", count));
    return "redirect:/manager/deposits";
  }
}
