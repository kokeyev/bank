package org.openbank.service.impl;

import org.openbank.service.DatabaseTransactionRunner;
import org.openbank.service.DepositService;
import org.openbank.service.MessageService;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.dao.DepositDao;
import org.openbank.dao.DepositTypeDao;
import org.openbank.dao.TransactionDao;
import org.openbank.dto.OpenDepositRequest;
import org.openbank.model.Account;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;
import org.openbank.model.status.DepositStatus;
import org.openbank.model.status.TransactionType;
import org.openbank.service.strategy.deposit.CapitalDepositStrategy;
import org.openbank.service.strategy.deposit.DepositProductStrategy;
import org.openbank.service.strategy.deposit.DepositProductStrategyResolver;
import org.openbank.service.strategy.deposit.KopilkaDepositStrategy;
import org.openbank.service.strategy.deposit.StrategyDepositStrategy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class DepositServiceImpl implements DepositService {

  private final DepositDao depositDao;
  private final DepositTypeDao depositTypeDao;
  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final TransactionDao transactionDao;
  private final DatabaseTransactionRunner transactionRunner;
  private final DepositProductStrategyResolver strategyResolver;
  private final MessageService messageService;

  public DepositServiceImpl(DepositDao depositDao, DepositTypeDao depositTypeDao, AccountDao accountDao, CurrencyDao currencyDao, TransactionDao transactionDao, DatabaseTransactionRunner transactionRunner, DepositProductStrategyResolver strategyResolver, MessageService messageService) {
    this.depositDao = depositDao;
    this.depositTypeDao = depositTypeDao;
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.transactionDao = transactionDao;
    this.transactionRunner = transactionRunner;
    this.strategyResolver = strategyResolver;
    this.messageService = messageService;
  }

  public List<DepositType> getDepositTypesByProduct(String productName) {
    List<DepositType> result = new ArrayList<>();
    for (DepositType type : depositTypeDao.getAllDepositTypes()) {
      if (type.getName().equals(productName)) {
        result.add(type);
      }
    }

    return result;
  }

  public List<Deposit> getDepositsByUserId(Long userId) {
    return depositDao.getDepositsByUserId(userId);
  }

  public Optional<DepositType> getDepositTypeById(Long depositTypeId) {
    return depositTypeDao.getDepositTypeById(depositTypeId);
  }

  public String getCurrencyNameById(Long currencyId) {
    return currencyDao.getCurrencyNameById(currencyId);
  }

  public boolean openDeposit(Long userId, OpenDepositRequest request) {
    validatePositive(request.getAmount(), messageService.get("deposit.validation.amount.positive"));

    DepositType depositType = depositTypeDao.getDepositTypeById(request.getDepositTypeId()).orElseThrow(() -> new IllegalArgumentException(messageService.get("validation.depositType.required")));
    DepositProductStrategy strategy = strategyResolver.resolve(depositType);

    strategy.validateOpeningAmount(depositType, request.getAmount());

    boolean autoRenewal = strategy.resolveAutoRenewal(request.getAutoRenewal());
    boolean reinvestInterest = strategy.resolveReinvestInterest(request.getReinvestInterest());

    return transactionRunner.run(messageService.get("deposit.operation.open.error"), connection -> {
      Account sourceAccount = getAccountForUpdate(connection, request.getSourceAccountId(), messageService.get("validation.senderAccount.required"));
      validateAccountBelongsToUser(sourceAccount, userId);
      validateSameCurrency(sourceAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(sourceAccount.getBalance().subtract(request.getAmount()), messageService.get("account.validation.insufficientFunds"));

      withdrawFromAccount(connection, sourceAccount.getAccountId(), request.getAmount());
      boolean created = depositDao.createDeposit(
          connection,
          userId,
          depositType.getDepositTypeId(),
          reinvestInterest,
          autoRenewal,
          DepositStatus.PENDING,
          LocalDate.now(),
          request.getAmount()
      );

      if (!created) {
        throw new IllegalStateException(messageService.get("deposit.operation.open.error"));
      }
      createTransactionHistory(connection, sourceAccount.getAccountId(), null, request.getAmount(), sourceAccount.getCurrencyId(), messageService.get("transaction.message.depositOpen", localizedDepositName(depositType.getName())), TransactionType.DEPOSIT_OPEN.name());

      return true;
    });
  }

  public boolean topUpDeposit(Long userId, Long sourceAccountId, Long depositId, BigDecimal amount) {
    validatePositive(amount, messageService.get("topUp.validation.amount.positive"));

    return transactionRunner.run(messageService.get("deposit.operation.topUp.error"), connection -> {
      Account sourceAccount = getAccountForUpdate(connection, sourceAccountId, messageService.get("validation.senderAccount.required"));
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.deposit.notFound")));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.depositType.notFound")));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);

      validateAccountBelongsToUser(sourceAccount, userId);
      validateDepositBelongsToUser(deposit, userId);
      validateActiveDeposit(deposit);
      validateTopUpAllowed(strategy);
      validateSameCurrency(sourceAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(sourceAccount.getBalance().subtract(amount), messageService.get("account.validation.insufficientFunds"));

      withdrawFromAccount(connection, sourceAccount.getAccountId(), amount);
      if (!depositDao.topUpDeposit(connection, deposit.getDepositId(), amount)) {
        throw new IllegalStateException(messageService.get("deposit.operation.topUp.error"));
      }
      createTransactionHistory(connection, sourceAccount.getAccountId(), null, amount, sourceAccount.getCurrencyId(), messageService.get("transaction.message.depositTopUp", depositId), TransactionType.DEPOSIT_TOP_UP.name());

      return true;
    });
  }

  public boolean withdrawFromDeposit(Long userId, Long depositId, Long targetAccountId, BigDecimal amount) {
    validatePositive(amount, messageService.get("withdraw.validation.amount.positive"));

    return transactionRunner.run(messageService.get("deposit.operation.withdraw.error"), connection -> {
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId).orElseThrow(() -> new IllegalArgumentException(messageService.get("error.deposit.notFound")));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId()).orElseThrow(() -> new IllegalArgumentException(messageService.get("error.depositType.notFound")));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      Account targetAccount = getAccountForUpdate(connection, targetAccountId, messageService.get("validation.targetAccount.required"));

      validateDepositBelongsToUser(deposit, userId);
      validateAccountBelongsToUser(targetAccount, userId);
      validateActiveDeposit(deposit);
      validateWithdrawalAllowed(strategy, depositType);
      validateSameCurrency(targetAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(deposit.getCurrentAmount().subtract(amount), messageService.get("deposit.validation.insufficientFunds"));

      if (!depositDao.withdrawFromDeposit(connection, depositId, amount)) {
        throw new IllegalStateException(messageService.get("deposit.operation.withdraw.error"));
      }
      topUpAccount(connection, targetAccountId, amount);
      createTransactionHistory(connection, null, targetAccountId, amount, targetAccount.getCurrencyId(), messageService.get("transaction.message.depositWithdrawal", depositId), TransactionType.DEPOSIT_WITHDRAWAL.name());

      return true;
    });
  }

  public List<Deposit> getPendingDeposits() {
    return depositDao.getPendingDeposits();
  }

  public boolean approveDeposit(Long depositId) {
    return depositDao.acceptDeposit(depositId);
  }

  public boolean rejectDeposit(Long depositId) {
    return transactionRunner.run(messageService.get("deposit.operation.reject.error"), connection -> {
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.deposit.notFound")));
      validatePendingDeposit(deposit);

      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.depositType.notFound")));
      Account refundAccount = accountDao.getFirstActiveAccountByUserIdAndCurrencyIdForUpdate(connection, deposit.getUserId(), depositType.getCurrencyId())
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.account.refundNotFound")));

      if (!depositDao.setStatus(connection, depositId, DepositStatus.REJECTED)) {
        throw new IllegalStateException(messageService.get("deposit.operation.reject.error"));
      }
      topUpAccount(connection, refundAccount.getAccountId(), deposit.getCurrentAmount());
      createTransactionHistory(connection, null, refundAccount.getAccountId(), deposit.getCurrentAmount(), depositType.getCurrencyId(), messageService.get("transaction.message.depositRejectionRefund", depositId), TransactionType.DEPOSIT_REJECTION_REFUND.name());

      return true;
    });
  }

  public int accrueInterestForActiveDeposits() {
    int updated = 0;

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.depositType.notFound")));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      BigDecimal monthlyInterest = strategy.calculateMonthlyInterest(deposit, depositType);

      if (monthlyInterest.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      transactionRunner.run(messageService.get("deposit.operation.accrueInterest.error"), connection -> {
        if (Boolean.TRUE.equals(deposit.getReinvestInterest())) {
          depositDao.topUpDeposit(connection, deposit.getDepositId(), monthlyInterest);
        }
        createTransactionHistory(connection, null, null, monthlyInterest, depositType.getCurrencyId(), messageService.get("transaction.message.depositInterest", deposit.getDepositId()), TransactionType.DEPOSIT_INTEREST.name());

        return null;
      });
      updated++;
    }

    return updated;
  }

  public int processExpiredDeposits() {
    int updated = 0;
    LocalDate today = LocalDate.now();

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException(messageService.get("error.depositType.notFound")));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      LocalDate endDate = strategy.maturityDate(deposit, depositType);
      if (endDate == null) {
        continue;
      }
      if (endDate.isAfter(today)) {
        continue;
      }

      transactionRunner.run(messageService.get("deposit.operation.processExpiration.error"), connection -> {
        if (Boolean.TRUE.equals(deposit.getAutoRenewal())) {
          depositDao.updateStartDate(connection, deposit.getDepositId(), today);
        } else {
          depositDao.setStatus(connection, deposit.getDepositId(), DepositStatus.EXPIRED);
        }

        return null;
      });
      updated++;
    }

    return updated;
  }

  public boolean canTopUpDeposit(Deposit deposit, DepositType depositType) {
    return DepositStatus.ACTIVE.name().equals(deposit.getStatus()) && strategyResolver.resolve(depositType).canTopUp();
  }

  public boolean canWithdrawDeposit(DepositType depositType) {
    return strategyResolver.resolve(depositType).canWithdraw(depositType);
  }

  private Account getAccountForUpdate(Connection connection, Long accountId, String missingMessage) {
    if (accountId == null) {
      throw new IllegalArgumentException(missingMessage);
    }

    return accountDao.getAccountByIdForUpdate(connection, accountId).orElseThrow(() -> new IllegalArgumentException(messageService.get("error.account.notFound")));
  }

  private void withdrawFromAccount(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.withdraw(connection, accountId, amount)) {
      throw new IllegalStateException(messageService.get("account.operation.withdraw.error"));
    }
  }

  private void topUpAccount(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.topUp(connection, accountId, amount)) {
      throw new IllegalStateException(messageService.get("account.operation.topUp.error"));
    }
  }

  private void validateAccountBelongsToUser(Account account, Long userId) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException(messageService.get("account.validation.notOwner"));
    }
  }

  private void validateDepositBelongsToUser(Deposit deposit, Long userId) {
    if (!deposit.getUserId().equals(userId)) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.notOwner"));
    }
  }

  private void validateSameCurrency(Long accountCurrencyId, Long depositCurrencyId) {
    if (!accountCurrencyId.equals(depositCurrencyId)) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.currencyMismatch"));
    }
  }

  private void validateActiveDeposit(Deposit deposit) {
    if (!DepositStatus.ACTIVE.name().equals(deposit.getStatus())) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.activeOnly"));
    }
  }

  private void validatePendingDeposit(Deposit deposit) {
    if (!DepositStatus.PENDING.name().equals(deposit.getStatus())) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.pendingOnly"));
    }
  }

  private void validateTopUpAllowed(DepositProductStrategy strategy) {
    if (!strategy.canTopUp()) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.topUpNotAllowed"));
    }
  }

  private void validateWithdrawalAllowed(DepositProductStrategy strategy, DepositType depositType) {
    if (!strategy.canWithdraw(depositType)) {
      throw new IllegalArgumentException(messageService.get("deposit.validation.withdrawalNotAllowed"));
    }
  }

  private void createTransactionHistory(Connection connection, Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, String message, String transactionType) {
    boolean created = transactionDao.createNewTransaction(
        connection,
        senderAccountId,
        receiverAccountId,
        LocalDateTime.now(),
        amount,
        currencyId,
        BigDecimal.ZERO,
        message,
        transactionType
    );

    if (!created) {
      throw new IllegalStateException(messageService.get("transaction.operation.history.error"));
    }
  }

  private String localizedDepositName(String productName) {
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

  private void validatePositive(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private void validatePositiveOrZero(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(message);
    }
  }

}
