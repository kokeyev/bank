package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.AccountTopUpRequest;
import org.openbank.dto.CardTransferRequest;
import org.openbank.dto.DepositTopUpRequest;
import org.openbank.dto.LoanPaymentRequest;
import org.openbank.dto.PhoneTransferRequest;
import org.openbank.dto.TransferBetweenAccountsRequest;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.DepositService;
import org.openbank.service.MessageService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class TransfersController {

  private static final int TRANSACTION_PAGE_SIZE = 10;

  private final CurrentUserService currentUserService;
  private final TransactionService transactionService;
  private final DepositService depositService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public TransfersController(CurrentUserService currentUserService, TransactionService transactionService, DepositService depositService, BankViewService bankViewService, MessageService messageService) {
    this.currentUserService = currentUserService;
    this.transactionService = transactionService;
    this.depositService = depositService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/transfers")
  public String transfers(@RequestParam(value = "page", defaultValue = "1") int page, HttpSession session, Model model) {
    currentUserService.getCurrentUser(session).ifPresent(user -> model.addAttribute("transactionsPage", bankViewService.getTransactionViewsPage(user.getUserId(), page, TRANSACTION_PAGE_SIZE)));

    return "transfers/index";
  }

  @GetMapping("/transfers/between-accounts")
  public String transferBetweenAccounts(HttpSession session, Model model) {
    addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());

    return "transfers/between-accounts";
  }

  @PostMapping("/transfers/between-accounts")
  public String createTransferBetweenAccounts(@Valid @ModelAttribute TransferBetweenAccountsRequest transferRequest, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addBetweenAccountsModelAttributes(session, model, transferRequest);

      return "transfers/between-accounts";
    }

    try {
      transactionService.makeTransactionBetweenAccountsOfOneClient(currentUser.get().getUserId(), transferRequest.getSenderAccountId(), transferRequest.getReceiverAccountId(), transferRequest.getAmount(), transferRequest.getMessage());

      addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
      model.addAttribute("transferSuccess", messageService.get("transfers.success"));
    } catch (IllegalArgumentException e) {
      addBetweenAccountsModelAttributes(session, model, transferRequest);
      model.addAttribute("transferError", e.getMessage());
    }

    return "transfers/between-accounts";
  }

  @GetMapping("/transfers/account-top-up")
  public String accountTopUp(HttpSession session, Model model) {
    addAccountTopUpModelAttributes(session, model, new AccountTopUpRequest());

    return "transfers/account-top-up";
  }

  @PostMapping("/transfers/account-top-up")
  public String createAccountTopUp(@Valid @ModelAttribute AccountTopUpRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addAccountTopUpModelAttributes(session, model, request);

      return "transfers/account-top-up";
    }

    try {
      transactionService.topUpAccount(currentUser.get().getUserId(), request.getAccountId(), request.getAmount());
      addAccountTopUpModelAttributes(session, model, new AccountTopUpRequest());
      model.addAttribute("accountTopUpSuccess", messageService.get("transfers.accountTopUp.success"));
    } catch (IllegalArgumentException e) {
      addAccountTopUpModelAttributes(session, model, request);
      model.addAttribute("accountTopUpError", e.getMessage());
    }

    return "transfers/account-top-up";
  }

  @GetMapping("/transfers/by-phone")
  public String transferByPhone(HttpSession session, Model model) {
    addPhoneTransferModelAttributes(session, model, new PhoneTransferRequest());

    return "transfers/by-phone";
  }

  @PostMapping("/transfers/by-phone")
  public String createTransferByPhone(@Valid @ModelAttribute PhoneTransferRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addPhoneTransferModelAttributes(session, model, request);

      return "transfers/by-phone";
    }

    try {
      transactionService.makeTransactionByPhoneNumber(request.getSenderAccountId(), request.getReceiverPhoneNumber(), request.getAmount());
      addPhoneTransferModelAttributes(session, model, new PhoneTransferRequest());
      model.addAttribute("phoneTransferSuccess", messageService.get("transfers.success"));
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
      model.addAttribute("cardTransferSuccess", messageService.get("transfers.success"));
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
  public String createDepositTopUp(@Valid @ModelAttribute DepositTopUpRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addDepositTopUpModelAttributes(session, model, request);

      return "transfers/deposit-top-up";
    }

    try {
      depositService.topUpDeposit(currentUser.get().getUserId(), request.getSourceAccountId(), request.getDepositId(), request.getAmount());
      addDepositTopUpModelAttributes(session, model, new DepositTopUpRequest());
      model.addAttribute("depositTopUpSuccess", messageService.get("transfers.deposit.success"));
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
  public String createLoanPayment(@Valid @ModelAttribute LoanPaymentRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addLoanPaymentModelAttributes(session, model, request);

      return "transfers/loan-payment";
    }

    try {
      transactionService.makeTransactionTopUpLoan(request.getSourceAccountId(), request.getLoanId(), request.getAmount());
      addLoanPaymentModelAttributes(session, model, new LoanPaymentRequest());
      model.addAttribute("loanPaymentSuccess", messageService.get("transfers.loan.success"));
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
  public String createCurrencyExchange(@Valid @ModelAttribute TransferBetweenAccountsRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addBetweenAccountsModelAttributes(session, model, request);

      return "transfers/currency-exchange";
    }

    try {
      transactionService.makeTransactionExchangeCurrencies(request.getSenderAccountId(), request.getReceiverAccountId(), request.getAmount());
      addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
      model.addAttribute("exchangeSuccess", messageService.get("transfers.exchange.success"));
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

  private void addAccountTopUpModelAttributes(HttpSession session, Model model, AccountTopUpRequest request) {
    model.addAttribute("accountTopUpRequest", request);
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
