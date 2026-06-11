package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.AccountView;
import org.author.demo.dto.DepositView;
import org.author.demo.dto.LoanView;
import org.author.demo.dto.OpenAccountRequest;
import org.author.demo.dto.TransactionView;
import org.author.demo.dto.TransferAccountOption;
import org.author.demo.model.Account;
import org.author.demo.model.Deposit;
import org.author.demo.model.DepositType;
import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.model.Transaction;
import org.author.demo.model.User;
import org.author.demo.model.status.AccountStatus;
import org.author.demo.model.status.DepositStatus;
import org.author.demo.model.status.LoanStatus;
import org.author.demo.services.AccountService;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.DepositService;
import org.author.demo.services.LoanService;
import org.author.demo.services.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class AccountsController {

  private static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");
  private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final DepositService depositService;
  private final LoanService loanService;
  private final TransactionService transactionService;

  public AccountsController(CurrentUserService currentUserService, AccountService accountService, DepositService depositService, LoanService loanService, TransactionService transactionService) {
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.depositService = depositService;
    this.loanService = loanService;
    this.transactionService = transactionService;
  }

  @GetMapping({"/", "/accounts"})
  public String accounts(HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    currentUser.ifPresent(user -> {
      model.addAttribute("clientName", user.getName());
      model.addAttribute("accounts", getAccountViews(user.getUserId()));
      model.addAttribute("deposits", getDepositViews(user.getUserId()));
      model.addAttribute("loans", getLoanViews(user.getUserId()));
      model.addAttribute("transactions", getTransactionViews(user.getUserId()));
      model.addAttribute("accountOptions", getTransferAccountOptions(user.getUserId()));
    });

    return "accounts/index";
  }

  @PostMapping("/deposits/{depositId}/withdraw")
  public String withdrawFromDeposit(
      @PathVariable Long depositId,
      @RequestParam("targetAccountId") Long targetAccountId,
      @RequestParam("amount") BigDecimal amount,
      HttpSession session,
      RedirectAttributes redirectAttributes
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      depositService.withdrawFromDeposit(currentUser.get().getUserId(), depositId, targetAccountId, amount);
      redirectAttributes.addFlashAttribute("depositSuccess", "Деньги сняты с депозита.");
    } catch (RuntimeException e) {
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
  public String createAccount(
      @ModelAttribute OpenAccountRequest openAccountRequest,
      HttpSession session,
      Model model
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.createNewAccount(
          currentUser.get().getUserId(),
          openAccountRequest.getCurrency(),
          openAccountRequest.getTransactionLimit(),
          openAccountRequest.getAccountName()
      );

      return "redirect:/accounts?accountRequested=true";
    } catch (RuntimeException e) {
      addOpenAccountModelAttributes(model, openAccountRequest);
      model.addAttribute("accountOpenError", e.getMessage());
      return "accounts/open";
    }
  }

  @PostMapping("/accounts/{accountId}/limit")
  public String updateLimit(
      @PathVariable Long accountId,
      @RequestParam("transactionLimit") BigDecimal transactionLimit,
      HttpSession session,
      RedirectAttributes redirectAttributes
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.updateTransactionLimit(currentUser.get().getUserId(), accountId, transactionLimit);
      redirectAttributes.addFlashAttribute("accountSuccess", "Лимит счета обновлен.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  @PostMapping("/accounts/{accountId}/main")
  public String makeMainAccount(@PathVariable Long accountId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.makeMainAccount(currentUser.get().getUserId(), accountId);
      redirectAttributes.addFlashAttribute("accountSuccess", "Основной счет обновлен.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  @PostMapping("/accounts/{accountId}/deactivate")
  public String deactivateAccount(@PathVariable Long accountId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      accountService.deactivateAccount(currentUser.get().getUserId(), accountId);
      redirectAttributes.addFlashAttribute("accountSuccess", "Счет деактивирован.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("accountError", e.getMessage());
    }

    return "redirect:/accounts";
  }

  private void addOpenAccountModelAttributes(Model model, OpenAccountRequest openAccountRequest) {
    model.addAttribute("openAccountRequest", openAccountRequest);
    model.addAttribute("currencies", accountService.getAllCurrencies());
  }

  private List<AccountView> getAccountViews(Long userId) {
    return accountService.getAccountsByUserId(userId)
        .stream()
        .map(this::toAccountView)
        .toList();
  }

  private AccountView toAccountView(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());

    return new AccountView(
        account.getAccountId(),
        account.getName(),
        formatMoney(account.getBalance()),
        currency,
        formatCardNumber(account.getCardNumber()),
        formatExpiryDate(account.getExpiryDate()),
        account.getCvv(),
        formatMoney(account.getTransactionLimit()),
        formatDecimalValue(account.getTransactionLimit()),
        account.getStatus(),
        translateAccountStatus(account.getStatus()),
        Boolean.TRUE.equals(account.getMain()),
        AccountStatus.ACTIVE.name().equals(account.getStatus())
    );
  }

  private List<DepositView> getDepositViews(Long userId) {
    return depositService.getDepositsByUserId(userId)
        .stream()
        .map(this::toDepositView)
        .toList();
  }

  private DepositView toDepositView(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());

    return new DepositView(
        deposit.getDepositId(),
        depositType.getName(),
        formatMoney(deposit.getCurrentAmount()),
        formatDecimalValue(deposit.getCurrentAmount()),
        currency,
        formatRate(depositType.getRate()),
        depositType.getDuration() + " мес.",
        formatDisplayDate(deposit.getStartDate()),
        translateStatus(deposit.getStatus()),
        formatBoolean(deposit.getAutoRenewal()),
        formatBoolean(deposit.getReinvestInterest()),
        DepositStatus.ACTIVE.name().equals(deposit.getStatus()),
        Boolean.TRUE.equals(depositType.getWithdrawal())
    );
  }

  private List<LoanView> getLoanViews(Long userId) {
    return loanService.getLoansByUserId(userId)
        .stream()
        .map(this::toLoanView)
        .toList();
  }

  private LoanView toLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));

    return new LoanView(
        loan.getLoanId(),
        loanType.getName(),
        formatMoney(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatRate(loan.getRate()),
        loan.getDuration() == null ? "-" : formatDuration(loan.getDuration()),
        loan.getMonthlyPayment() == null ? "-" : formatMoney(loan.getMonthlyPayment()) + " ₸",
        translateLoanStatus(loan.getStatus()),
        formatDisplayDate(loan.getStartDate()),
        LoanStatus.OFFERED.name().equals(loan.getStatus())
    );
  }

  private List<TransactionView> getTransactionViews(Long userId) {
    return transactionService.getRecentTransactionsByUserId(userId, 6)
        .stream()
        .map(this::toTransactionView)
        .toList();
  }

  private TransactionView toTransactionView(Transaction transaction) {
    String currency = accountService.getCurrencyNameById(transaction.getCurrencyId());
    return new TransactionView(
        transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
        translateTransactionType(transaction.getTransactionType()),
        formatMoney(transaction.getAmount()) + " " + currency,
        formatMoney(transaction.getFee()),
        transaction.getMessage()
    );
  }

  private List<TransferAccountOption> getTransferAccountOptions(Long userId) {
    return accountService.getAccountsByUserId(userId)
        .stream()
        .filter(account -> AccountStatus.ACTIVE.name().equals(account.getStatus()))
        .map(account -> new TransferAccountOption(account.getAccountId(), account.getName() + " - " + formatMoney(account.getBalance()) + " " + accountService.getCurrencyNameById(account.getCurrencyId())))
        .toList();
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

  private String formatDecimalValue(BigDecimal amount) {
    return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
  }

  private String formatCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      return "";
    }

    return cardNumber.replaceAll("(.{4})(?!$)", "$1 ");
  }

  private String formatExpiryDate(LocalDate expiryDate) {
    return expiryDate == null ? "" : expiryDate.format(EXPIRY_DATE_FORMATTER);
  }

  private String formatDisplayDate(LocalDate date) {
    return date == null ? "" : date.format(DISPLAY_DATE_FORMATTER);
  }

  private String formatRate(BigDecimal rate) {
    if (rate == null) {
      return "0%";
    }

    return rate.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
  }

  private String formatBoolean(Boolean value) {
    return Boolean.TRUE.equals(value) ? "Да" : "Нет";
  }

  private String translateStatus(String status) {
    if (DepositStatus.PENDING.name().equals(status)) {
      return "На рассмотрении";
    }
    if (DepositStatus.ACTIVE.name().equals(status)) {
      return "Активен";
    }
    if (DepositStatus.EXPIRED.name().equals(status)) {
      return "Срок истек";
    }
    if (DepositStatus.REJECTED.name().equals(status)) {
      return "Отклонен";
    }
    if (DepositStatus.CLOSED.name().equals(status)) {
      return "Закрыт";
    }

    return status == null ? "" : status;
  }

  private String translateLoanStatus(String status) {
    if (LoanStatus.PENDING.name().equals(status)) {
      return "На рассмотрении";
    }
    if (LoanStatus.OFFERED.name().equals(status)) {
      return "Предложение";
    }
    if (LoanStatus.ACTIVE.name().equals(status)) {
      return "Активен";
    }
    if (LoanStatus.REFUSED.name().equals(status)) {
      return "Отклонен клиентом";
    }
    if (LoanStatus.REJECTED.name().equals(status)) {
      return "Отклонен менеджером";
    }
    if (LoanStatus.CLOSED.name().equals(status)) {
      return "Закрыт";
    }
    return status == null ? "" : status;
  }

  private String translateTransactionType(String type) {
    if ("BETWEEN_OWN_ACCOUNTS".equals(type)) {
      return "Между своими счетами";
    }
    if ("PHONE_TRANSFER".equals(type)) {
      return "По телефону";
    }
    if ("CARD_TRANSFER".equals(type)) {
      return "На карту";
    }
    if ("EXTERNAL_CARD_TRANSFER".equals(type)) {
      return "В другой банк";
    }
    if ("CURRENCY_EXCHANGE".equals(type)) {
      return "Обмен валют";
    }
    if ("LOAN_PAYMENT".equals(type)) {
      return "Платеж по кредиту";
    }
    if ("DEPOSIT_OPEN".equals(type)) {
      return "Открытие депозита";
    }
    if ("DEPOSIT_TOP_UP".equals(type)) {
      return "Пополнение депозита";
    }
    if ("DEPOSIT_WITHDRAWAL".equals(type)) {
      return "Снятие с депозита";
    }
    if ("DEPOSIT_INTEREST".equals(type)) {
      return "Вознаграждение";
    }
    return type == null ? "" : type;
  }

  private String formatDuration(Integer duration) {
    if (duration == null) {
      return "-";
    }
    return duration + " мес.";
  }

  private String translateAccountStatus(String status) {
    if (AccountStatus.PENDING.name().equals(status)) {
      return "На рассмотрении";
    }
    if (AccountStatus.ACTIVE.name().equals(status)) {
      return "Активен";
    }
    if (AccountStatus.DEACTIVATED.name().equals(status)) {
      return "Деактивирован";
    }
    if (AccountStatus.EXPIRED.name().equals(status)) {
      return "Истек";
    }
    if (AccountStatus.DELETED.name().equals(status)) {
      return "Удален";
    }
    if (AccountStatus.REJECTED.name().equals(status)) {
      return "Отклонен";
    }

    return status == null ? "" : status;
  }
}
