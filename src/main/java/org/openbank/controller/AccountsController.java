package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.OpenAccountRequest;
import org.openbank.model.User;
import org.openbank.service.AccountService;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Optional;

@Controller
public class AccountsController {

  private static final int TRANSACTION_PAGE_SIZE = 6;

  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final DepositService depositService;
  private final BankViewService bankViewService;

  public AccountsController(CurrentUserService currentUserService, AccountService accountService, DepositService depositService, BankViewService bankViewService) {
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.depositService = depositService;
    this.bankViewService = bankViewService;
  }

  @GetMapping({"/", "/accounts"})
  public String accounts(@RequestParam(value = "transactionsPage", defaultValue = "1") int transactionsPage, HttpSession session, Model model) {

    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isPresent()) {
      User user = currentUser.get();
      Long userId = user.getUserId();
      model.addAttribute("clientName", user.getName());
      model.addAttribute("accounts", bankViewService.getAccountViews(userId));
      model.addAttribute("deposits", bankViewService.getDepositViews(userId));
      model.addAttribute("loans", bankViewService.getLoanViews(userId));
      model.addAttribute("transactionsPage", bankViewService.getTransactionViewsPage(userId, transactionsPage, TRANSACTION_PAGE_SIZE));
      model.addAttribute("accountOptions", bankViewService.getTransferAccountOptions(userId));
    }

    return "accounts/index";
  }

  @PostMapping("/deposits/{depositId}/withdraw")
  public String withdrawFromDeposit(@PathVariable("depositId") Long depositId, @RequestParam("targetAccountId") Long targetAccountId, @RequestParam("amount") BigDecimal amount, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      depositService.withdrawFromDeposit(currentUser.get().getUserId(), depositId, targetAccountId, amount);
      redirectAttributes.addFlashAttribute("depositSuccess", "Деньги сняты с депозита.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("depositError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  @GetMapping("/accounts/open")
  public String openAccount(Model model) {
    addOpenAccountModelAttributes(model, new OpenAccountRequest());
    return "accounts/open";
  }

  @PostMapping("/accounts/open")
  public String createAccount(@Valid @ModelAttribute OpenAccountRequest openAccountRequest, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addOpenAccountModelAttributes(model, openAccountRequest);
      model.addAttribute("accountOpenErrors", bindingResult.getFieldErrors().stream()
          .map(error -> error.getDefaultMessage())
          .toList());
      return "accounts/open";
    }

    try {
      accountService.createNewAccount(
          currentUser.get().getUserId(),
          openAccountRequest.getCurrency(),
          openAccountRequest.getTransactionLimit(),
          openAccountRequest.getAccountName()
      );

      return "redirect:/accounts?accountRequested=true";
    } catch (IllegalArgumentException e) {
      addOpenAccountModelAttributes(model, openAccountRequest);
      model.addAttribute("accountOpenError", e.getMessage());

      return "accounts/open";
    }
  }

  @PostMapping("/accounts/{accountId}/limit")
  public String updateLimit(@PathVariable("accountId") Long accountId, @RequestParam("transactionLimit") BigDecimal transactionLimit, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.updateTransactionLimit(currentUser.get().getUserId(), accountId, transactionLimit);
      redirectAttributes.addFlashAttribute("accountSuccess", "Лимит счета обновлен.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  @PostMapping("/accounts/{accountId}/main")
  public String makeMainAccount(@PathVariable("accountId") Long accountId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.makeMainAccount(currentUser.get().getUserId(), accountId);
      redirectAttributes.addFlashAttribute("accountSuccess", "Основной счет обновлен.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  @PostMapping("/accounts/{accountId}/deactivate")
  public String deactivateAccount(@PathVariable("accountId") Long accountId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.deactivateAccount(currentUser.get().getUserId(), accountId);
      redirectAttributes.addFlashAttribute("accountSuccess", "Счет деактивирован.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  private void addOpenAccountModelAttributes(Model model, OpenAccountRequest openAccountRequest) {
    model.addAttribute("openAccountRequest", openAccountRequest);
    model.addAttribute("currencies", accountService.getAllCurrencies());
  }
}
