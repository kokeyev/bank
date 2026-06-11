package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.CardTransferRequest;
import org.author.demo.dto.DepositOption;
import org.author.demo.dto.DepositTopUpRequest;
import org.author.demo.dto.LoanOption;
import org.author.demo.dto.LoanPaymentRequest;
import org.author.demo.dto.PhoneTransferRequest;
import org.author.demo.dto.TransferAccountOption;
import org.author.demo.dto.TransferBetweenAccountsRequest;
import org.author.demo.dto.TransactionView;
import org.author.demo.model.Account;
import org.author.demo.model.Deposit;
import org.author.demo.model.DepositType;
import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.model.Transaction;
import org.author.demo.model.User;
import org.author.demo.model.status.AccountStatus;
import org.author.demo.model.status.DepositStatus;
import org.author.demo.services.AccountService;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.DepositService;
import org.author.demo.services.LoanService;
import org.author.demo.services.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class TransfersController {

  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final TransactionService transactionService;
  private final DepositService depositService;
  private final LoanService loanService;

  public TransfersController(CurrentUserService currentUserService, AccountService accountService, TransactionService transactionService, DepositService depositService, LoanService loanService) {
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.transactionService = transactionService;
    this.depositService = depositService;
    this.loanService = loanService;
  }

  @GetMapping("/transfers")
  public String transfers(HttpSession session, Model model) {
    currentUserService.getCurrentUser(session)
        .ifPresent(user -> model.addAttribute("transactions", getTransactionViews(user.getUserId())));
    return "transfers/index";
  }

  @GetMapping("/transfers/between-accounts")
  public String transferBetweenAccounts(HttpSession session, Model model) {
    addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
    return "transfers/between-accounts";
  }

  @PostMapping("/transfers/between-accounts")
  public String createTransferBetweenAccounts(
      @ModelAttribute TransferBetweenAccountsRequest transferRequest,
      HttpSession session,
      Model model
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionBetweenAccountsOfOneClient(
          currentUser.get().getUserId(),
          transferRequest.getSenderAccountId(),
          transferRequest.getReceiverAccountId(),
          transferRequest.getAmount(),
          transferRequest.getMessage()
      );

      addBetweenAccountsModelAttributes(session, model, new TransferBetweenAccountsRequest());
      model.addAttribute("transferSuccess", "Перевод выполнен.");
    } catch (RuntimeException e) {
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
    } catch (RuntimeException e) {
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
  public String createTransferByCard(@ModelAttribute CardTransferRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionByCardNumber(request.getSenderAccountId(), request.getReceiverCardNumber(), request.getAmount());
      addCardTransferModelAttributes(session, model, new CardTransferRequest());
      model.addAttribute("cardTransferSuccess", "Перевод выполнен.");
    } catch (RuntimeException e) {
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
    } catch (RuntimeException e) {
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
    } catch (RuntimeException e) {
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
    } catch (RuntimeException e) {
      addBetweenAccountsModelAttributes(session, model, request);
      model.addAttribute("exchangeError", e.getMessage());
    }

    return "transfers/currency-exchange";
  }

  private void addBetweenAccountsModelAttributes(HttpSession session, Model model, TransferBetweenAccountsRequest transferRequest) {
    model.addAttribute("transferRequest", transferRequest);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private List<TransferAccountOption> getAccountOptions(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return List.of();
    }

    return accountService.getAccountsByUserId(currentUser.get().getUserId())
        .stream()
        .filter(account -> AccountStatus.ACTIVE.name().equals(account.getStatus()))
        .map(this::toTransferAccountOption)
        .toList();
  }

  private void addPhoneTransferModelAttributes(HttpSession session, Model model, PhoneTransferRequest request) {
    model.addAttribute("phoneTransferRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private void addCardTransferModelAttributes(HttpSession session, Model model, CardTransferRequest request) {
    model.addAttribute("cardTransferRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
  }

  private void addLoanPaymentModelAttributes(HttpSession session, Model model, LoanPaymentRequest request) {
    model.addAttribute("loanPaymentRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
    model.addAttribute("loanOptions", getLoanOptions(session));
  }

  private List<LoanOption> getLoanOptions(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return List.of();
    }

    return loanService.getActiveLoansByUserId(currentUser.get().getUserId())
        .stream()
        .map(this::toLoanOption)
        .toList();
  }

  private LoanOption toLoanOption(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));
    String label = loanType.getName() + " - остаток " + formatMoney(loan.getRemainingAmount()) + " KZT";
    return new LoanOption(loan.getLoanId(), label);
  }

  private void addDepositTopUpModelAttributes(HttpSession session, Model model, DepositTopUpRequest request) {
    model.addAttribute("depositTopUpRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
    model.addAttribute("depositOptions", getDepositOptions(session));
  }

  private List<DepositOption> getDepositOptions(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return List.of();
    }

    return depositService.getDepositsByUserId(currentUser.get().getUserId())
        .stream()
        .filter(this::canTopUpDeposit)
        .map(this::toDepositOption)
        .toList();
  }

  private boolean canTopUpDeposit(Deposit deposit) {
    if (!DepositStatus.ACTIVE.name().equals(deposit.getStatus())) {
      return false;
    }

    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    return !"Капитал".equals(depositType.getName());
  }

  private DepositOption toDepositOption(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = depositType.getName()
        + " "
        + depositType.getDuration()
        + " мес. - "
        + formatMoney(deposit.getCurrentAmount())
        + " "
        + currency;

    return new DepositOption(deposit.getDepositId(), label);
  }

  private TransferAccountOption toTransferAccountOption(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());
    String label = account.getName() + " - " + formatMoney(account.getBalance()) + " " + currency;

    return new TransferAccountOption(account.getAccountId(), label);
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');

    DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
    return format.format(amount);
  }

  private List<TransactionView> getTransactionViews(Long userId) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    return transactionService.getRecentTransactionsByUserId(userId, 20)
        .stream()
        .map(transaction -> toTransactionView(transaction, formatter))
        .toList();
  }

  private TransactionView toTransactionView(Transaction transaction, DateTimeFormatter formatter) {
    String currency = accountService.getCurrencyNameById(transaction.getCurrencyId());
    String date = transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate().format(formatter);
    return new TransactionView(
        date,
        translateTransactionType(transaction.getTransactionType()),
        formatMoney(transaction.getAmount()) + " " + currency,
        formatMoney(transaction.getFee()),
        transaction.getMessage()
    );
  }

  private String translateTransactionType(String type) {
    if ("BETWEEN_OWN_ACCOUNTS".equals(type)) {
      return "Между своими счетами";
    }
    if ("PHONE_TRANSFER".equals(type)) {
      return "По телефону";
    }
    if ("CARD_TRANSFER".equals(type)) {
      return "По карте";
    }
    if ("EXTERNAL_CARD_TRANSFER".equals(type)) {
      return "На внешнюю карту";
    }
    if ("CURRENCY_EXCHANGE".equals(type)) {
      return "Обмен валют";
    }
    if ("LOAN_PAYMENT".equals(type)) {
      return "Погашение кредита";
    }
    return type == null ? "" : type;
  }
}
