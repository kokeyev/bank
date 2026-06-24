package org.openbank.view;

import org.openbank.dto.AccountView;
import org.openbank.dto.DepositOption;
import org.openbank.dto.DepositTypeOption;
import org.openbank.dto.DepositView;
import org.openbank.dto.LoanOption;
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
import org.openbank.service.AccountService;
import org.openbank.service.DepositService;
import org.openbank.service.LoanService;
import org.openbank.service.MessageService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BankViewService {

  private final AccountService accountService;
  private final DepositService depositService;
  private final LoanService loanService;
  private final TransactionService transactionService;
  private final BankViewMapper mapper;
  private final MessageService messageService;

  public BankViewService(AccountService accountService, DepositService depositService, LoanService loanService, TransactionService transactionService, BankViewMapper mapper, MessageService messageService) {
    this.accountService = accountService;
    this.depositService = depositService;
    this.loanService = loanService;
    this.transactionService = transactionService;
    this.mapper = mapper;
    this.messageService = messageService;
  }

  public List<AccountView> getAccountViews(Long userId) {
    List<AccountView> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      result.add(mapper.toAccountView(account));
    }
    return result;
  }

  public List<AccountView> getPendingAccountViews() {
    List<AccountView> result = new ArrayList<>();
    for (Account account : accountService.getPendingAccounts()) {
      result.add(mapper.toAccountView(account));
    }
    return result;
  }

  public Page<AccountView> getPendingAccountViewsPage(int page, int size) {
    return new Page<>(getPendingAccountViews(), page, size);
  }

  public List<DepositView> getDepositViews(Long userId) {
    List<DepositView> result = new ArrayList<>();
    for (Deposit deposit : depositService.getDepositsByUserId(userId)) {
      result.add(mapper.toDepositView(deposit));
    }
    return result;
  }

  public Page<DepositView> getDepositViewsPage(Long userId, int page, int size) {
    return new Page<>(getDepositViews(userId), page, size);
  }

  public List<DepositView> getPendingDepositViews() {
    List<DepositView> result = new ArrayList<>();
    for (Deposit deposit : depositService.getPendingDeposits()) {
      result.add(mapper.toDepositView(deposit));
    }
    return result;
  }

  public Page<DepositView> getPendingDepositViewsPage(int page, int size) {
    return new Page<>(getPendingDepositViews(), page, size);
  }

  public List<LoanView> getLoanViews(Long userId) {
    List<LoanView> result = new ArrayList<>();
    for (Loan loan : loanService.getLoansByUserId(userId)) {
      result.add(mapper.toLoanView(loan));
    }
    return result;
  }

  public Page<LoanView> getLoanViewsPage(Long userId, int page, int size) {
    return new Page<>(getLoanViews(userId), page, size);
  }

  public List<LoanView> getPendingLoanViews() {
    List<LoanView> result = new ArrayList<>();
    for (Loan loan : loanService.getPendingLoans()) {
      result.add(mapper.toPendingLoanView(loan));
    }
    return result;
  }

  public Page<LoanView> getPendingLoanViewsPage(int page, int size) {
    return new Page<>(getPendingLoanViews(), page, size);
  }

  public List<LoanTypeView> getLoanTypeViews() {
    List<LoanTypeView> result = new ArrayList<>();
    for (LoanType loanType : loanService.getAllLoanTypes()) {
      result.add(mapper.toLoanTypeView(loanType));
    }
    return result;
  }

  public LoanTypeView getLoanTypeView(String loanTypeName) {
    Optional<LoanType> loanTypeOptional = loanService.getLoanTypeByName(loanTypeName);
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalStateException(messageService.get("error.loanType.notFound"));
    }
    return mapper.toLoanTypeView(loanTypeOptional.get());
  }

  public List<TransactionView> getTransactionViews(Long userId, int limit) {
    List<TransactionView> result = new ArrayList<>();
    for (Transaction transaction : transactionService.getRecentTransactionsByUserId(userId, limit)) {
      result.add(mapper.toTransactionView(transaction));
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
      items.add(mapper.toTransactionView(transaction));
    }

    return new Page<>(items, currentPage, safeSize, total);
  }

  public List<TransferAccountOption> getTransferAccountOptions(Long userId) {
    List<TransferAccountOption> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      if (AccountStatus.ACTIVE.name().equals(account.getStatus())) {
        result.add(mapper.toTransferAccountOption(account));
      }
    }

    return result;
  }

  public List<TransferAccountOption> getKztAccountOptions(Long userId) {
    List<TransferAccountOption> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      if (AccountStatus.ACTIVE.name().equals(account.getStatus()) && "KZT".equals(accountService.getCurrencyNameById(account.getCurrencyId()))) {
        result.add(mapper.toTransferAccountOption(account));
      }
    }

    return result;
  }

  public List<TransferAccountOption> getAllAccountOptions(Long userId) {
    List<TransferAccountOption> result = new ArrayList<>();
    for (Account account : accountService.getAccountsByUserId(userId)) {
      result.add(mapper.toTransferAccountOption(account));
    }

    return result;
  }

  public List<DepositTypeOption> getDepositTypeOptions(String productName) {
    List<DepositTypeOption> result = new ArrayList<>();
    for (DepositType depositType : depositService.getDepositTypesByProduct(productName)) {
      result.add(mapper.toDepositTypeOption(depositType));
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
    return mapper.formatMoney(amount);
  }

  public List<LoanOption> getLoanOptions(Long userId) {
    List<LoanOption> result = new ArrayList<>();
    for (Loan loan : loanService.getActiveLoansByUserId(userId)) {
      result.add(mapper.toLoanOption(loan));
    }
    return result;
  }

  public List<DepositOption> getDepositOptions(Long userId) {
    List<DepositOption> result = new ArrayList<>();
    for (Deposit deposit : depositService.getDepositsByUserId(userId)) {
      if (canTopUpDeposit(deposit)) {
        result.add(mapper.toDepositOption(deposit));
      }
    }
    return result;
  }

  private boolean canTopUpDeposit(Deposit deposit) {
    if (!DepositStatus.ACTIVE.name().equals(deposit.getStatus())) {
      return false;
    }

    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException(messageService.get("error.depositType.notFound")));
    return depositService.canTopUpDeposit(deposit, depositType);
  }

}
