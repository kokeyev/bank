package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.CardTransferRequest;
import org.openbank.dto.DepositTopUpRequest;
import org.openbank.dto.LoanPaymentRequest;
import org.openbank.dto.PhoneTransferRequest;
import org.openbank.dto.TransferBetweenAccountsRequest;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.DepositService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class TransfersController {

  private final CurrentUserService currentUserService;
  private final TransactionService transactionService;
  private final DepositService depositService;
  private final BankViewService bankViewService;

  public TransfersController(CurrentUserService currentUserService, TransactionService transactionService, DepositService depositService, BankViewService bankViewService) {
    this.currentUserService = currentUserService;
    this.transactionService = transactionService;
    this.depositService = depositService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/transfers")
  public String transfers(HttpSession session, Model model) {
    currentUserService.getCurrentUser(session).ifPresent(user -> model.addAttribute("transactions", bankViewService.getTransactionViews(user.getUserId(), 20)));
    return "transfers/index";
  }

  @GetMapping("/transfers/between-accounts")
  public String transferBetweenAccounts(HttpSession session, Model model) {
    addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
    return "transfers/between-accounts";
  }

  @PostMapping("/transfers/between-accounts")
  public String createTransferBetweenAccounts(@ModelAttribute TransferBetweenAccountsRequest transferRequest, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionBetweenAccountsOfOneClient(currentUser.get().getUserId(), transferRequest.getSenderAccountId(), transferRequest.getReceiverAccountId(), transferRequest.getAmount(), transferRequest.getMessage());

      addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
      model.addAttribute("transferSuccess", "Перевод выполнен.");
    } catch (IllegalArgumentException e) {
      addBetweenAccountsModelAttributes(session, model, transferRequest);
      model.addAttribute("transferError", e.getMessage());
    }

    return "transfers/between-accounts";
  }

  @GetMapping("/transfers/by-phone")
  public String transferByPhone(HttpSession session, Model model) {
    addPhoneTransferModelAttributes(session, model, new PhoneTransferRequest());
    return "transfers/by-phone";
  }

  @PostMapping("/transfers/by-phone")
  public String createTransferByPhone(@ModelAttribute PhoneTransferRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionByPhoneNumber(request.getSenderAccountId(), request.getReceiverPhoneNumber(), request.getAmount());
      addPhoneTransferModelAttributes(session, model, new PhoneTransferRequest());
      model.addAttribute("phoneTransferSuccess", "Перевод выполнен.");
    } catch (IllegalArgumentException e) {
      addPhoneTransferModelAttributes(session, model, request);
      model.addAttribute("phoneTransferError", e.getMessage());
    }

    return "transfers/by-phone";
  }

  @GetMapping("/transfers/by-card")
  public String transferByCard(HttpSession session, Model model) {
    addCardTransferModelAttributes(session, model, new CardTransferRequest());
    return "transfers/by-card";
  }

  @PostMapping("/transfers/by-card")
  public String createTransferByCard(@Valid @ModelAttribute CardTransferRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addCardTransferModelAttributes(session, model, request);
      return "transfers/by-card";
    }

    try {
      transactionService.makeTransactionByCardNumber(request.getSenderAccountId(), request.getReceiverCardNumber(), request.getAmount());
      addCardTransferModelAttributes(session, model, new CardTransferRequest());
      model.addAttribute("cardTransferSuccess", "Перевод выполнен.");
    } catch (IllegalArgumentException e) {
      addCardTransferModelAttributes(session, model, request);
      model.addAttribute("cardTransferError", e.getMessage());
    }

    return "transfers/by-card";
  }

  @GetMapping("/transfers/deposit-top-up")
  public String depositTopUp(HttpSession session, Model model) {
    addDepositTopUpModelAttributes(session, model, new DepositTopUpRequest());
    return "transfers/deposit-top-up";
  }

  @PostMapping("/transfers/deposit-top-up")
  public String createDepositTopUp(@ModelAttribute DepositTopUpRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      depositService.topUpDeposit(currentUser.get().getUserId(), request.getSourceAccountId(), request.getDepositId(), request.getAmount());
      addDepositTopUpModelAttributes(session, model, new DepositTopUpRequest());
      model.addAttribute("depositTopUpSuccess", "Депозит пополнен.");
    } catch (IllegalArgumentException e) {
      addDepositTopUpModelAttributes(session, model, request);
      model.addAttribute("depositTopUpError", e.getMessage());
    }

    return "transfers/deposit-top-up";
  }

  @GetMapping("/transfers/loan-payment")
  public String loanPayment(HttpSession session, Model model) {
    addLoanPaymentModelAttributes(session, model, new LoanPaymentRequest());
    return "transfers/loan-payment";
  }

  @PostMapping("/transfers/loan-payment")
  public String createLoanPayment(@ModelAttribute LoanPaymentRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionTopUpLoan(request.getSourceAccountId(), request.getLoanId(), request.getAmount());
      addLoanPaymentModelAttributes(session, model, new LoanPaymentRequest());
      model.addAttribute("loanPaymentSuccess", "Платеж по кредиту выполнен.");
    } catch (IllegalArgumentException e) {
      addLoanPaymentModelAttributes(session, model, request);
      model.addAttribute("loanPaymentError", e.getMessage());
    }

    return "transfers/loan-payment";
  }

  @GetMapping("/transfers/currency-exchange")
  public String currencyExchange(HttpSession session, Model model) {
    addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
    return "transfers/currency-exchange";
  }

  @PostMapping("/transfers/currency-exchange")
  public String createCurrencyExchange(@ModelAttribute TransferBetweenAccountsRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionExchangeCurrencies(request.getSenderAccountId(), request.getReceiverAccountId(), request.getAmount());
      addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
      model.addAttribute("exchangeSuccess", "Обмен выполнен.");
    } catch (IllegalArgumentException e) {
      addBetweenAccountsModelAttributes(session, model, request);
      model.addAttribute("exchangeError", e.getMessage());
    }

    return "transfers/currency-exchange";
  }

  private void addBetweenAccountsModelAttributes(HttpSession session, Model model, TransferBetweenAccountsRequest transferRequest) {
    model.addAttribute("transferRequest", transferRequest);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private void addPhoneTransferModelAttributes(HttpSession session, Model model, PhoneTransferRequest request) {
    model.addAttribute("phoneTransferRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private void addCardTransferModelAttributes(HttpSession session, Model model, CardTransferRequest request) {
    model.addAttribute("cardTransferRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private void addDepositTopUpModelAttributes(HttpSession session, Model model, DepositTopUpRequest request) {
    model.addAttribute("depositTopUpRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
    Optional<Long> userId = currentUserId(session);
    if (userId.isPresent()) {
      model.addAttribute("depositOptions", bankViewService.getDepositOptions(userId.get()));
    } else {
      model.addAttribute("depositOptions", List.of());
    }
  }

  private void addLoanPaymentModelAttributes(HttpSession session, Model model, LoanPaymentRequest request) {
    model.addAttribute("loanPaymentRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
    Optional<Long> userId = currentUserId(session);
    if (userId.isPresent()) {
      model.addAttribute("loanOptions", bankViewService.getLoanOptions(userId.get()));
    } else {
      model.addAttribute("loanOptions", List.of());
    }
  }

  private List<?> getAccountOptions(HttpSession session) {
    Optional<Long> userId = currentUserId(session);
    if (userId.isPresent()) {
      return bankViewService.getTransferAccountOptions(userId.get());
    }
    return List.of();
  }

  private Optional<Long> currentUserId(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isPresent()) {
      return Optional.of(currentUser.get().getUserId());
    }
    return Optional.empty();
  }
}
