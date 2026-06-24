package org.openbank.view;

import org.openbank.dto.AccountView;
import org.openbank.dto.DepositOption;
import org.openbank.dto.DepositTypeOption;
import org.openbank.dto.DepositView;
import org.openbank.dto.LoanOption;
import org.openbank.dto.LoanPaymentScheduleItem;
import org.openbank.dto.LoanTypeView;
import org.openbank.dto.LoanView;
import org.openbank.dto.TransactionView;
import org.openbank.dto.TransferAccountOption;
import org.openbank.model.Account;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.Transaction;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.DepositStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.model.status.TransactionType;
import org.openbank.service.AccountService;
import org.openbank.service.DepositService;
import org.openbank.service.LoanService;
import org.openbank.service.MessageService;
import org.openbank.service.strategy.deposit.CapitalDepositStrategy;
import org.openbank.service.strategy.deposit.KopilkaDepositStrategy;
import org.openbank.service.strategy.deposit.StrategyDepositStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class BankViewMapper {

  private static final DateTimeFormatter TRANSACTION_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

  private final AccountService accountService;
  private final DepositService depositService;
  private final LoanService loanService;
  private final BankDisplayFormatter formatter;
  private final LoanProductText loanProductText;
  private final MessageService messageService;

  public BankViewMapper(AccountService accountService, DepositService depositService, LoanService loanService, BankDisplayFormatter formatter, LoanProductText loanProductText, MessageService messageService) {
    this.accountService = accountService;
    this.depositService = depositService;
    this.loanService = loanService;
    this.formatter = formatter;
    this.loanProductText = loanProductText;
    this.messageService = messageService;
  }

  public AccountView toAccountView(Account account) {
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

  public DepositView toDepositView(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId()).orElseThrow(() -> new IllegalStateException(messageService.get("error.depositType.notFound")));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());

    return new DepositView(
        deposit.getDepositId(),
        depositName(depositType.getName()),
        formatter.money(deposit.getCurrentAmount()),
        formatter.decimalValue(deposit.getCurrentAmount()),
        currency,
        formatter.rate(depositType.getRate()),
        formatter.duration(depositType.getDuration()),
        formatter.displayDate(deposit.getStartDate()),
        formatter.depositStatus(deposit.getStatus()),
        formatter.yesNo(deposit.getAutoRenewal()),
        formatter.yesNo(deposit.getReinvestInterest()),
        DepositStatus.ACTIVE.name().equals(deposit.getStatus()),
        depositService.canWithdrawDeposit(depositType)
    );
  }

  public LoanView toLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId()).orElseThrow(() -> new IllegalStateException(messageService.get("error.loanType.notFound")));

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

  public LoanView toPendingLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException(messageService.get("error.loanType.notFound")));

    return new LoanView(
        loan.getLoanId(),
        loanProductText.name(loanType.getName()),
        formatter.money(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatter.rate(loan.getRate()),
        loan.getDuration() == null ? "-" : formatter.duration(loan.getDuration()),
        loan.getMonthlyPayment() == null ? "-" : formatter.money(loan.getMonthlyPayment()) + " ₸",
        messageService.get("status.pending"),
        "-",
        false
    );
  }

  public LoanTypeView toLoanTypeView(LoanType loanType) {
    return new LoanTypeView(
        loanType.getLoanTypeId(),
        loanProductText.name(loanType.getName()),
        loanProductText.urlPath(loanType.getName()),
        loanProductText.tag(loanType.getName()),
        loanProductText.description(loanType.getName()),
        loanProductText.amountRange(formatter.money(loanType.getMinimumAmount()), formatter.money(loanType.getMaximumAmount())),
        loanProductText.durationUpTo(loanType.getDuration()),
        loanProductText.rateFrom(formatter.rate(loanType.getRate()))
    );
  }

  public TransactionView toTransactionView(Transaction transaction) {
    String currency = accountService.getCurrencyNameById(transaction.getCurrencyId());
    String date = transaction.getTransactionDate() == null ? "" : transaction.getTransactionDate().format(TRANSACTION_DATE_FORMATTER);

    return new TransactionView(
        date,
        TransactionType.displayName(transaction.getTransactionType(), messageService),
        formatter.money(transaction.getAmount()) + " " + currency,
        formatter.money(transaction.getFee()),
        transaction.getMessage()
    );
  }

  public TransferAccountOption toTransferAccountOption(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());

    return new TransferAccountOption(account.getAccountId(), account.getName() + " - " + formatter.money(account.getBalance()) + " " + currency);
  }

  public LoanOption toLoanOption(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId()).orElseThrow(() -> new IllegalStateException(messageService.get("error.loanType.notFound")));

    return new LoanOption(loan.getLoanId(), loanProductText.remainingAmount(loanType.getName(), formatter.money(loan.getRemainingAmount())));
  }

  public DepositOption toDepositOption(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId()).orElseThrow(() -> new IllegalStateException(messageService.get("error.depositType.notFound")));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = messageService.get("deposits.option.label", depositName(depositType.getName()), formatter.duration(depositType.getDuration()), formatter.money(deposit.getCurrentAmount()), currency);

    return new DepositOption(deposit.getDepositId(), label);
  }

  public DepositTypeOption toDepositTypeOption(DepositType depositType) {
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = messageService.get("deposits.typeOption.label", formatter.duration(depositType.getDuration()), currency, depositType.getRate(), formatter.money(depositType.getMinimumAmount()));

    return new DepositTypeOption(depositType.getDepositTypeId(), label);
  }

  public String formatMoney(BigDecimal amount) {
    return formatter.money(amount);
  }

  private List<LoanPaymentScheduleItem> getScheduleItems(Loan loan) {
    if (!LoanStatus.ACTIVE.name().equals(loan.getStatus()) || loan.getMonthlyPayment() == null) {
      return List.of();
    }

    List<LocalDate> dueDates = loanService.getPaymentDueDates(loan);
    List<LoanPaymentScheduleItem> result = new ArrayList<>();

    for (int i = 0; i < dueDates.size(); i++) {
      LocalDate dueDate = dueDates.get(i);
      String status = dueDate.isBefore(LocalDate.now()) ? messageService.get("loans.schedule.checkPayment") : messageService.get("loans.schedule.expected");
      result.add(new LoanPaymentScheduleItem(
          i + 1,
          formatter.displayDate(dueDate),
          formatter.money(loan.getMonthlyPayment()) + " ₸",
          status
      ));
    }

    return result;
  }

  private String depositName(String productName) {
    if (KopilkaDepositStrategy.PRODUCT_NAME.equals(productName)) {
      return messageService.get("deposits.kopilka.name");
    }
    if (StrategyDepositStrategy.PRODUCT_NAME.equals(productName)) {
      return messageService.get("deposits.strategy.name");
    }
    if (CapitalDepositStrategy.PRODUCT_NAME.equals(productName)) {
      return messageService.get("deposits.capital.name");
    }

    return productName == null ? "" : productName;
  }
}
