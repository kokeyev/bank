package org.openbank.controller;

import org.openbank.service.AccountService;
import org.openbank.service.MessageService;
import org.openbank.view.BankViewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ManagerAccountsController {

  private static final int PAGE_SIZE = 5;

  private final AccountService accountService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public ManagerAccountsController(AccountService accountService, BankViewService bankViewService, MessageService messageService) {
    this.accountService = accountService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/manager/accounts")
  public String pendingAccounts(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
    model.addAttribute("pendingAccountsPage", bankViewService.getPendingAccountViewsPage(page, PAGE_SIZE));

    return "manager/accounts";
  }

  @PostMapping("/manager/accounts/{accountId}/approve")
  public String approveAccount(@PathVariable("accountId") Long accountId, RedirectAttributes redirectAttributes) {
    try {
      accountService.approveAccount(accountId);
      redirectAttributes.addFlashAttribute("managerAccountSuccess", messageService.get("manager.accounts.approve.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("managerAccountError", e.getMessage());
    }

    return "redirect:/manager/accounts";
  }

  @PostMapping("/manager/accounts/{accountId}/reject")
  public String rejectAccount(@PathVariable("accountId") Long accountId, RedirectAttributes redirectAttributes) {
    try {
      accountService.rejectAccount(accountId);
      redirectAttributes.addFlashAttribute("managerAccountSuccess", messageService.get("manager.accounts.reject.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("managerAccountError", e.getMessage());
    }

    return "redirect:/manager/accounts";
  }
}
