package org.openbank.view;

import org.openbank.dto.AccountView;
import org.openbank.dto.DepositOption;
import org.openbank.dto.DepositTypeOption;
import org.openbank.dto.DepositView;
import org.openbank.dto.LoanOption;
import org.openbank.dto.LoanPaymentScheduleItem;
import org.openbank.dto.LoanTypeView;
import org.openbank.dto.LoanView;
import org.openbank.dto.Page;
import org.openbank.dto.TransactionView;
import org.openbank.dto.TransferAccountOption;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.Transaction;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.DepositStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.AccountService;
import org.openbank.service.DepositService;
import org.openbank.service.LoanService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankViewService {

  private static final String CAPITAL = "Капитал";
  private static final DateTimeFormatter TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final AccountService accountService;
  private final DepositService depositService;
  private final LoanService loanService;
  private final TransactionService transactionService;
  private final BankDisplayFormatter formatter;
  private final TransactionTypeFormatter transactionTypeFormatter;
  private final LoanProductText loanProductText;

  public BankViewService(AccountService accountService, DepositService depositService, LoanService loanService, TransactionService transactionService, BankDisplayFormatter formatter, TransactionTypeFormatter transactionTypeFormatter, LoanProductText loanProductText) {
    this.accountService = accountService;
    this.depositService = depositService;
    this.loanService = loanService;
    this.transactionService = transactionService;
    this.formatter = formatter;
    this.transactionTypeFormatter = transactionTypeFormatter;
    this.loanProductText = loanProductText;
  }

  public List<AccountView> getAccountViews(Long userId) {
    List<AccountView> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      result.add(toAccountView(account));
    }
    return result;
  }

  public List<AccountView> getPendingAccountViews() {
    List<AccountView> result = new ArrayList<>();
    for (Account account : accountService.getPendingAccounts()) {
      result.add(toAccountView(account));
    }
    return result;
  }

  public Page<AccountView> getPendingAccountViewsPage(int page, int size) {
    return new Page<>(getPendingAccountViews(), page, size);
  }

  public List<DepositView> getDepositViews(Long userId) {
    List<DepositView> result = new ArrayList<>();
    for (Deposit deposit : depositService.getDepositsByUserId(userId)) {
      result.add(toDepositView(deposit));
    }
    return result;
  }

  public List<DepositView> getPendingDepositViews() {
    List<DepositView> result = new ArrayList<>();
    for (Deposit deposit : depositService.getPendingDeposits()) {
      result.add(toDepositView(deposit));
    }
    return result;
  }

  public List<LoanView> getLoanViews(Long userId) {
    List<LoanView> result = new ArrayList<>();
    for (Loan loan : loanService.getLoansByUserId(userId)) {
      result.add(toLoanView(loan));
    }
    return result;
  }

  public List<LoanView> getPendingLoanViews() {
    List<LoanView> result = new ArrayList<>();
    for (Loan loan : loanService.getPendingLoans()) {
      result.add(toPendingLoanView(loan));
    }
    return result;
  }

  public List<LoanTypeView> getLoanTypeViews() {
    List<LoanTypeView> result = new ArrayList<>();
    for (LoanType loanType : loanService.getAllLoanTypes()) {
      result.add(toLoanTypeView(loanType));
    }
    return result;
  }

  public LoanTypeView getLoanTypeView(String loanTypeName) {
    Optional<LoanType> loanTypeOptional = loanService.getLoanTypeByName(loanTypeName);
    if (!loanTypeOptional.isPresent()) {
      throw new IllegalStateException("Тип кредита не найден");
    }
    return toLoanTypeView(loanTypeOptional.get());
  }

  public List<TransactionView> getTransactionViews(Long userId, int limit) {
    List<TransactionView> result = new ArrayList<>();
    for (Transaction transaction : transactionService.getRecentTransactionsByUserId(userId, limit)) {
      result.add(toTransactionView(transaction));
    }
    return result;
  }

  public Page<TransactionView> getTransactionViewsPage(Long userId, int page, int size) {
    int safeSize = Math.max(size, 1);
    int total = transactionService.countTransactionsByUserId(userId);
    int totalPages = Math.max(1, (int) Math.ceil((double) total / safeSize));
    int currentPage = Math.min(Math.max(page, 1), totalPages);
    int offset = (currentPage - 1) * safeSize;

    List<TransactionView> items = new ArrayList<>();
    for (Transaction transaction : transactionService.getTransactionsByUserId(userId, safeSize, offset)) {
      items.add(toTransactionView(transaction));
    }

    return new Page<>(items, currentPage, safeSize, total);
  }

  public List<TransferAccountOption> getTransferAccountOptions(Long userId) {
    List<TransferAccountOption> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      if (AccountStatus.ACTIVE.name().equals(account.getStatus())) {
        result.add(toTransferAccountOption(account));
      }
    }
    return result;
  }

  public List<TransferAccountOption> getAllAccountOptions(Long userId) {
    List<TransferAccountOption> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      result.add(toTransferAccountOption(account));
    }
    return result;
  }

  public List<DepositTypeOption> getDepositTypeOptions(String productName) {
    List<DepositTypeOption> result = new ArrayList<>();
    for (DepositType depositType : depositService.getDepositTypesByProduct(productName)) {
      result.add(toDepositTypeOption(depositType));
    }
    return result;
  }

  public List<String> getMissingCurrencyNames(Long userId, List<Currency> currencies) {
    List<Long> userCurrencyIds = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      if (AccountStatus.ACTIVE.name().equals(account.getStatus())) {
        if (!userCurrencyIds.contains(account.getCurrencyId())) {
          userCurrencyIds.add(account.getCurrencyId());
        }
      }
    }

    List<String> result = new ArrayList<>();
    for (Currency currency : currencies) {
      if (!userCurrencyIds.contains(currency.getCurrencyId())) {
        result.add(currency.getName());
      }
    }
    return result;
  }

  public String formatMoney(BigDecimal amount) {
    return formatter.money(amount);
  }

  public List<LoanOption> getLoanOptions(Long userId) {
    List<LoanOption> result = new ArrayList<>();
    for (Loan loan : loanService.getActiveLoansByUserId(userId)) {
      result.add(toLoanOption(loan));
    }
    return result;
  }

  public List<DepositOption> getDepositOptions(Long userId) {
    List<DepositOption> result = new ArrayList<>();
    for (Deposit deposit : depositService.getDepositsByUserId(userId)) {
      if (canTopUpDeposit(deposit)) {
        result.add(toDepositOption(deposit));
      }
    }
    return result;
  }

  private AccountView toAccountView(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());

    return new AccountView(
        account.getAccountId(),
        account.getName(),
        formatter.money(account.getBalance()),
        currency,
        formatter.cardNumber(account.getCardNumber()),
        formatter.expiryDate(account.getExpiryDate()),
        account.getCvv(),
        formatter.money(account.getTransactionLimit()),
        formatter.decimalValue(account.getTransactionLimit()),
        account.getStatus(),
        formatter.accountStatus(account.getStatus()),
        Boolean.TRUE.equals(account.getMain()),
        AccountStatus.ACTIVE.name().equals(account.getStatus())
    );
  }

  private DepositView toDepositView(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());

    return new DepositView(
        deposit.getDepositId(),
        depositType.getName(),
        formatter.money(deposit.getCurrentAmount()),
        formatter.decimalValue(deposit.getCurrentAmount()),
        currency,
        formatter.rate(depositType.getRate()),
        depositType.getDuration() + " мес.",
        formatter.displayDate(deposit.getStartDate()),
        formatter.depositStatus(deposit.getStatus()),
        formatter.yesNo(deposit.getAutoRenewal()),
        formatter.yesNo(deposit.getReinvestInterest()),
        DepositStatus.ACTIVE.name().equals(deposit.getStatus()),
        Boolean.TRUE.equals(depositType.getWithdrawal())
    );
  }

  private LoanView toLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));

    return new LoanView(
        loan.getLoanId(),
        loanProductText.name(loanType.getName()),
        formatter.money(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatter.rate(loan.getRate()),
        loan.getDuration() == null ? "-" : formatter.duration(loan.getDuration()),
        loan.getMonthlyPayment() == null ? "-" : formatter.money(loan.getMonthlyPayment()) + " ₸",
        formatter.loanStatus(loan.getStatus()),
        formatter.displayDateOrDash(loan.getStartDate()),
        LoanStatus.OFFERED.name().equals(loan.getStatus()),
        LoanStatus.ACTIVE.name().equals(loan.getStatus()),
        formatter.money(loanService.calculateLatePenalty(loan)) + " ₸",
        getScheduleItems(loan)
    );
  }

  private LoanView toPendingLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));

    return new LoanView(
        loan.getLoanId(),
        loanProductText.name(loanType.getName()),
        formatter.money(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatter.rate(loan.getRate()),
        loan.getDuration() == null ? "-" : loan.getDuration() + " мес.",
        loan.getMonthlyPayment() == null ? "-" : formatter.money(loan.getMonthlyPayment()) + " ₸",
        "На рассмотрении",
        "-",
        false
    );
  }

  private LoanTypeView toLoanTypeView(LoanType loanType) {
    return new LoanTypeView(
        loanType.getLoanTypeId(),
        loanProductText.name(loanType.getName()),
        loanProductText.slug(loanType.getName()),
        loanProductText.tag(loanType.getName()),
        loanProductText.description(loanType.getName()),
        loanProductText.amountRange(formatter.money(loanType.getMinimumAmount()), formatter.money(loanType.getMaximumAmount())),
        loanProductText.durationUpTo(loanType.getDuration()),
        loanProductText.rateFrom(formatter.rate(loanType.getRate()))
    );
  }

  private TransactionView toTransactionView(Transaction transaction) {
    String currency = accountService.getCurrencyNameById(transaction.getCurrencyId());
    String date = transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate().format(TRANSACTION_DATE_FORMATTER);

    return new TransactionView(
        date,
        transactionTypeFormatter.displayName(transaction.getTransactionType()),
        formatter.money(transaction.getAmount()) + " " + currency,
        formatter.money(transaction.getFee()),
        transaction.getMessage()
    );
  }

  private TransferAccountOption toTransferAccountOption(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());
    return new TransferAccountOption(account.getAccountId(), account.getName() + " - " + formatter.money(account.getBalance()) + " " + currency);
  }

  private LoanOption toLoanOption(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));
    return new LoanOption(loan.getLoanId(), loanProductText.remainingAmount(loanType.getName(), formatter.money(loan.getRemainingAmount())));
  }

  private DepositOption toDepositOption(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = depositType.getName() + " " + depositType.getDuration() + " мес. - " + formatter.money(deposit.getCurrentAmount()) + " " + currency;

    return new DepositOption(deposit.getDepositId(), label);
  }

  private DepositTypeOption toDepositTypeOption(DepositType depositType) {
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = depositType.getDuration() + " мес. / " + currency + " / " + depositType.getRate() + "% / минимум " + formatter.money(depositType.getMinimumAmount());

    return new DepositTypeOption(depositType.getDepositTypeId(), label);
  }

  private boolean canTopUpDeposit(Deposit deposit) {
    if (!DepositStatus.ACTIVE.name().equals(deposit.getStatus())) {
      return false;
    }

    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    return !CAPITAL.equals(depositType.getName());
  }

  private List<LoanPaymentScheduleItem> getScheduleItems(Loan loan) {
    if (!LoanStatus.ACTIVE.name().equals(loan.getStatus()) || loan.getMonthlyPayment() == null) {
      return List.of();
    }

    List<LocalDate> dueDates = loanService.getPaymentDueDates(loan);
    List<LoanPaymentScheduleItem> result = new ArrayList<>();

    for (int i = 0; i < dueDates.size(); i++) {
      LocalDate dueDate = dueDates.get(i);
      String status = dueDate.isBefore(LocalDate.now()) ? "Проверьте оплату" : "Ожидается";
      result.add(new LoanPaymentScheduleItem(
          i + 1,
          formatter.displayDate(dueDate),
          formatter.money(loan.getMonthlyPayment()) + " ₸",
          status
      ));
    }

    return result;
  }

}
