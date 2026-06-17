package org.openbank.service;

import org.openbank.dao.account.AccountDao;
import org.openbank.dao.currency.CurrencyDao;
import org.openbank.dao.deposit.DepositDao;
import org.openbank.dao.deposittype.DepositTypeDao;
import org.openbank.dao.transaction.TransactionDao;
import org.openbank.dto.OpenDepositRequest;
import org.openbank.model.Account;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;
import org.openbank.model.status.DepositStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Provides deposit service operations.
 */
@Service
public class DepositService {

  private static final String KOPILKA = "Копилка";
  private static final String STRATEGY = "Стратегия";
  private static final String CAPITAL = "Капитал";
  private static final String DEPOSIT_OPEN = "DEPOSIT_OPEN";
  private static final String DEPOSIT_TOP_UP = "DEPOSIT_TOP_UP";
  private static final String DEPOSIT_WITHDRAWAL = "DEPOSIT_WITHDRAWAL";
  private static final String DEPOSIT_INTEREST = "DEPOSIT_INTEREST";

  private final DepositDao depositDao;
  private final DepositTypeDao depositTypeDao;
  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final TransactionDao transactionDao;
  private final DatabaseTransactionRunner transactionRunner;

  /**
   * Handles deposit service.
   */
  public DepositService(DepositDao depositDao, DepositTypeDao depositTypeDao, AccountDao accountDao, CurrencyDao currencyDao, TransactionDao transactionDao, DatabaseTransactionRunner transactionRunner) {
    this.depositDao = depositDao;
    this.depositTypeDao = depositTypeDao;
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.transactionDao = transactionDao;
    this.transactionRunner = transactionRunner;
  }

  /**
   * Handles get deposit types by product.
   */
  public List<DepositType> getDepositTypesByProduct(String productName) {
    List<DepositType> result = new ArrayList<>();
    for (DepositType type : depositTypeDao.getAllDepositTypes()) {
      if (type.getName().equals(productName)) {
        result.add(type);
      }
    }
    return result;
  }

  /**
   * Handles get deposits by user id.
   */
  public List<Deposit> getDepositsByUserId(Long userId) {
    return depositDao.getDepositsByUserId(userId);
  }

  /**
   * Handles get deposit type by id.
   */
  public Optional<DepositType> getDepositTypeById(Long depositTypeId) {
    return depositTypeDao.getDepositTypeById(depositTypeId);
  }

  /**
   * Handles get currency name by id.
   */
  public String getCurrencyNameById(Long currencyId) {
    return currencyDao.getCurrencyNameById(currencyId);
  }

  /**
   * Handles open deposit.
   */
  public boolean openDeposit(Long userId, OpenDepositRequest request) {
    validatePositive(request.getAmount(), "Сумма депозита должна быть больше нуля");

    DepositType depositType = depositTypeDao.getDepositTypeById(request.getDepositTypeId())
        .orElseThrow(() -> new IllegalArgumentException("Выберите условия депозита"));

    validateDepositAmount(depositType, request.getAmount());

    boolean autoRenewal = resolveAutoRenewal(depositType.getName(), request.getAutoRenewal());
    boolean reinvestInterest = resolveReinvestInterest(depositType.getName(), request.getReinvestInterest());

    return transactionRunner.run("Не удалось открыть депозит", connection -> {
      Account sourceAccount = getAccountForUpdate(connection, request.getSourceAccountId(), "Выберите счет списания");
      validateAccountBelongsToUser(sourceAccount, userId);
      validateSameCurrency(sourceAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(sourceAccount.getBalance().subtract(request.getAmount()), "Не хватает средств на счете");

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
        throw new IllegalStateException("Не удалось открыть депозит");
      }
      createTransactionHistory(connection, sourceAccount.getAccountId(), null, request.getAmount(), sourceAccount.getCurrencyId(), "Открытие депозита " + depositType.getName(), DEPOSIT_OPEN);

      return true;
    });
  }

  /**
   * Handles top up deposit.
   */
  public boolean topUpDeposit(Long userId, Long sourceAccountId, Long depositId, BigDecimal amount) {
    validatePositive(amount, "Сумма пополнения должна быть больше нуля");

    return transactionRunner.run("Не удалось пополнить депозит", connection -> {
      Account sourceAccount = getAccountForUpdate(connection, sourceAccountId, "Выберите счет списания");
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException("Депозит не найден"));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));

      validateAccountBelongsToUser(sourceAccount, userId);
      validateDepositBelongsToUser(deposit, userId);
      validateActiveDeposit(deposit);
      validateTopUpAllowed(depositType);
      validateSameCurrency(sourceAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(sourceAccount.getBalance().subtract(amount), "Не хватает средств на счете");

      withdrawFromAccount(connection, sourceAccount.getAccountId(), amount);
      if (!depositDao.topUpDeposit(connection, deposit.getDepositId(), amount)) {
        throw new IllegalStateException("Не удалось пополнить депозит");
      }
      createTransactionHistory(connection, sourceAccount.getAccountId(), null, amount, sourceAccount.getCurrencyId(), "Пополнение депозита #" + depositId, DEPOSIT_TOP_UP);

      return true;
    });
  }

  /**
   * Handles withdraw from deposit.
   */
  public boolean withdrawFromDeposit(Long userId, Long depositId, Long targetAccountId, BigDecimal amount) {
    validatePositive(amount, "Сумма снятия должна быть больше нуля");

    return transactionRunner.run("Не удалось снять деньги с депозита", connection -> {
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException("Депозит не найден"));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      Account targetAccount = getAccountForUpdate(connection, targetAccountId, "Выберите счет зачисления");

      validateDepositBelongsToUser(deposit, userId);
      validateAccountBelongsToUser(targetAccount, userId);
      validateActiveDeposit(deposit);
      validateWithdrawalAllowed(depositType);
      validateSameCurrency(targetAccount.getCurrencyId(), depositType.getCurrencyId());
      validatePositiveOrZero(deposit.getCurrentAmount().subtract(amount), "На депозите недостаточно средств");

      if (!depositDao.withdrawFromDeposit(connection, depositId, amount)) {
        throw new IllegalStateException("Не удалось снять деньги с депозита");
      }
      topUpAccount(connection, targetAccountId, amount);
      createTransactionHistory(connection, null, targetAccountId, amount, targetAccount.getCurrencyId(), "Снятие с депозита #" + depositId, DEPOSIT_WITHDRAWAL);

      return true;
    });
  }

  /**
   * Handles get pending deposits.
   */
  public List<Deposit> getPendingDeposits() {
    return depositDao.getPendingDeposits();
  }

  /**
   * Handles approve deposit.
   */
  public boolean approveDeposit(Long depositId) {
    return depositDao.acceptDeposit(depositId);
  }

  /**
   * Handles reject deposit.
   */
  public boolean rejectDeposit(Long depositId) {
    return depositDao.setStatus(depositId, DepositStatus.REJECTED);
  }

  /**
   * Handles accrue interest for active deposits.
   */
  public int accrueInterestForActiveDeposits() {
    int updated = 0;

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      BigDecimal monthlyInterest = deposit.getCurrentAmount()
          .multiply(depositType.getRate())
          .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
          .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

      if (monthlyInterest.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      transactionRunner.run("Не удалось начислить вознаграждение", connection -> {
        if (Boolean.TRUE.equals(deposit.getReinvestInterest())) {
          depositDao.topUpDeposit(connection, deposit.getDepositId(), monthlyInterest);
        }
        createTransactionHistory(connection, null, null, monthlyInterest, depositType.getCurrencyId(), "Начисление вознаграждения по депозиту #" + deposit.getDepositId(), DEPOSIT_INTEREST);

        return null;
      });
      updated++;
    }

    return updated;
  }

  /**
   * Handles process expired deposits.
   */
  public int processExpiredDeposits() {
    int updated = 0;
    LocalDate today = LocalDate.now();

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      if (deposit.getStartDate() == null || depositType.getDuration() == null) {
        continue;
      }
      LocalDate endDate = deposit.getStartDate().plusMonths(depositType.getDuration());
      if (endDate.isAfter(today)) {
        continue;
      }

      transactionRunner.run("Не удалось обработать срок депозита", connection -> {
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

  private boolean resolveAutoRenewal(String productName, Boolean requestedAutoRenewal) {
    if (CAPITAL.equals(productName)) {
      return false;
    }

    return Boolean.TRUE.equals(requestedAutoRenewal);
  }

  private boolean resolveReinvestInterest(String productName, Boolean requestedReinvestInterest) {
    if (CAPITAL.equals(productName)) {
      return true;
    }

    return Boolean.TRUE.equals(requestedReinvestInterest);
  }

  private void validateDepositAmount(DepositType depositType, BigDecimal amount) {
    if (amount.compareTo(depositType.getMinimumAmount()) < 0) {
      throw new IllegalArgumentException("Минимальная сумма для выбранных условий: " + depositType.getMinimumAmount());
    }
  }

  private Account getAccountForUpdate(Connection connection, Long accountId, String missingMessage) {
    if (accountId == null) {
      throw new IllegalArgumentException(missingMessage);
    }

    return accountDao.getAccountByIdForUpdate(connection, accountId)
        .orElseThrow(() -> new IllegalArgumentException("Счет не найден"));
  }

  private void withdrawFromAccount(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.withdraw(connection, accountId, amount)) {
      throw new IllegalStateException("Не удалось списать деньги со счета");
    }
  }

  private void topUpAccount(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.topUp(connection, accountId, amount)) {
      throw new IllegalStateException("Не удалось зачислить деньги на счет");
    }
  }

  private void validateAccountBelongsToUser(Account account, Long userId) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Счет не принадлежит текущему пользователю");
    }
  }

  private void validateDepositBelongsToUser(Deposit deposit, Long userId) {
    if (!deposit.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Депозит не принадлежит текущему пользователю");
    }
  }

  private void validateSameCurrency(Long accountCurrencyId, Long depositCurrencyId) {
    if (!accountCurrencyId.equals(depositCurrencyId)) {
      throw new IllegalArgumentException("Валюта счета должна совпадать с валютой депозита");
    }
  }

  private void validateActiveDeposit(Deposit deposit) {
    if (!DepositStatus.ACTIVE.name().equals(deposit.getStatus())) {
      throw new IllegalArgumentException("Операции доступны только для активного депозита");
    }
  }

  private void validateTopUpAllowed(DepositType depositType) {
    if (CAPITAL.equals(depositType.getName())) {
      throw new IllegalArgumentException("Депозит Капитал нельзя пополнять");
    }
  }

  private void validateWithdrawalAllowed(DepositType depositType) {
    if (!Boolean.TRUE.equals(depositType.getWithdrawal())) {
      throw new IllegalArgumentException("Снятие доступно только для депозита Копилка");
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
      throw new IllegalStateException("Не удалось сохранить историю операции");
    }
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
