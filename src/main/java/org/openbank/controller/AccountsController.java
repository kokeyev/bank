package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.AccountLimitUpdateRequest;
import org.openbank.dto.DepositWithdrawRequest;
import org.openbank.dto.OpenAccountRequest;
import org.openbank.model.User;
import org.openbank.service.AccountService;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.DepositService;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class AccountsController {

  private static final int TRANSACTION_PAGE_SIZE = 6;
  private static final int DEPOSIT_PAGE_SIZE = 4;
  private static final int LOAN_PAGE_SIZE = 4;

  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final DepositService depositService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public AccountsController(CurrentUserService currentUserService, AccountService accountService, DepositService depositService, BankViewService bankViewService, MessageService messageService) {
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.depositService = depositService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping({"/", "/accounts"})
  public String accounts(@RequestParam(value = "depositsPage", defaultValue = "1") int depositsPage, @RequestParam(value = "loansPage", defaultValue = "1") int loansPage, @RequestParam(value = "transactionsPage", defaultValue = "1") int transactionsPage, HttpSession session, Model model) {

    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isPresent()) {
      User user = currentUser.get();
      Long userId = user.getUserId();
      model.addAttribute("clientName", user.getName());
      model.addAttribute("accounts", bankViewService.getAccountViews(userId));
      model.addAttribute("depositsPage", bankViewService.getDepositViewsPage(userId, depositsPage, DEPOSIT_PAGE_SIZE));
      model.addAttribute("loansPage", bankViewService.getLoanViewsPage(userId, loansPage, LOAN_PAGE_SIZE));
      model.addAttribute("transactionsPage", bankViewService.getTransactionViewsPage(userId, transactionsPage, TRANSACTION_PAGE_SIZE));
      model.addAttribute("accountOptions", bankViewService.getTransferAccountOptions(userId));
    }

    return "accounts/index";
  }

  @PostMapping("/deposits/{depositId}/withdraw")
  public String withdrawFromDeposit(@PathVariable("depositId") Long depositId, @Valid @ModelAttribute DepositWithdrawRequest request, BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("depositError", firstError(bindingResult));

      return "redirect:/accounts";
    }

    try {
      depositService.withdrawFromDeposit(currentUser.get().getUserId(), depositId, request.getTargetAccountId(), request.getAmount());
      redirectAttributes.addFlashAttribute("depositSuccess", messageService.get("accounts.deposit.withdraw.success"));
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
      List<String> errorMessages = new ArrayList<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        errorMessages.add(validationMessage(error));
      }
      model.addAttribute("accountOpenErrors", errorMessages);

      return "accounts/open";
    }

    try {
      accountService.createNewAccount(currentUser.get().getUserId(), openAccountRequest.getCurrency(), openAccountRequest.getTransactionLimit(), openAccountRequest.getAccountName());

      return "redirect:/accounts?accountRequested=true";
    } catch (IllegalArgumentException e) {
      addOpenAccountModelAttributes(model, openAccountRequest);
      model.addAttribute("accountOpenError", e.getMessage());

      return "accounts/open";
    }
  }

  @PostMapping("/accounts/{accountId}/limit")
  public String updateLimit(@PathVariable("accountId") Long accountId, @Valid @ModelAttribute AccountLimitUpdateRequest request, BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("accountError", firstError(bindingResult));

      return "redirect:/accounts";
    }

    try {
      accountService.updateTransactionLimit(currentUser.get().getUserId(), accountId, request.getTransactionLimit());
      redirectAttributes.addFlashAttribute("accountSuccess", messageService.get("accounts.limit.success"));
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
      redirectAttributes.addFlashAttribute("accountSuccess", messageService.get("accounts.main.success"));
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
      redirectAttributes.addFlashAttribute("accountSuccess", messageService.get("accounts.deactivate.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  private void addOpenAccountModelAttributes(Model model, OpenAccountRequest openAccountRequest) {
    model.addAttribute("openAccountRequest", openAccountRequest);
    model.addAttribute("currencies", accountService.getAllCurrencies());
  }

  private String firstError(BindingResult bindingResult) {
    FieldError error = bindingResult.getFieldErrors().getFirst();

    return validationMessage(error);
  }

  private String validationMessage(FieldError error) {
    String message = error.getDefaultMessage();
    if (message != null && message.startsWith("{") && message.endsWith("}")) {
      return messageService.get(message.substring(1, message.length() - 1));
    }

    return message;
  }
}
