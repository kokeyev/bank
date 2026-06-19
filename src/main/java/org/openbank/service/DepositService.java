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
import org.openbank.service.strategy.deposit.DepositProductStrategy;
import org.openbank.service.strategy.deposit.DepositProductStrategyResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Coordinates deposit products, account debits/credits, and deposit status changes.
 *
 * <p>Business behavior that depends on a deposit product is delegated to
 * {@link DepositProductStrategy}; this service keeps the transaction boundaries and ownership
 * checks in one place.</p>
 */
@Service
public class DepositService {

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
  private final DepositProductStrategyResolver strategyResolver;
  public DepositService(DepositDao depositDao, DepositTypeDao depositTypeDao, AccountDao accountDao, CurrencyDao currencyDao, TransactionDao transactionDao, DatabaseTransactionRunner transactionRunner, DepositProductStrategyResolver strategyResolver) {
    this.depositDao = depositDao;
    this.depositTypeDao = depositTypeDao;
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.transactionDao = transactionDao;
    this.transactionRunner = transactionRunner;
    this.strategyResolver = strategyResolver;
  }

  /**
   * Finds deposit terms for a product name such as Kopilka, Strategy, or Capital.
   *
   * @param productName product family name stored in deposit type records
   * @return matching deposit terms
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
   * Loads all deposits owned by a user.
   *
   * @param userId owner identifier
   * @return user's deposit records
   */
  public List<Deposit> getDepositsByUserId(Long userId) {
    return depositDao.getDepositsByUserId(userId);
  }

  /**
   * Finds a deposit type by identifier.
   *
   * @param depositTypeId deposit type primary key
   * @return deposit type when configured
   */
  public Optional<DepositType> getDepositTypeById(Long depositTypeId) {
    return depositTypeDao.getDepositTypeById(depositTypeId);
  }

  /**
   * Resolves a currency identifier to its display name.
   *
   * @param currencyId currency primary key
   * @return configured currency name
   */
  public String getCurrencyNameById(Long currencyId) {
    return currencyDao.getCurrencyNameById(currencyId);
  }

  /**
   * Opens a pending deposit and debits the selected source account in one database transaction.
   *
   * @param userId owner of the deposit and source account
   * @param request selected product terms, source account, amount, and product options
   * @return {@code true} when the deposit request and history record are created
   * @throws IllegalArgumentException when validation, ownership, currency, or balance checks fail
   * @throws IllegalStateException when one of the database updates inside the transaction fails
   */
  public boolean openDeposit(Long userId, OpenDepositRequest request) {
    validatePositive(request.getAmount(), "Сумма депозита должна быть больше нуля");

    DepositType depositType = depositTypeDao.getDepositTypeById(request.getDepositTypeId())
        .orElseThrow(() -> new IllegalArgumentException("Выберите условия депозита"));
    DepositProductStrategy strategy = strategyResolver.resolve(depositType);

    strategy.validateOpeningAmount(depositType, request.getAmount());

    boolean autoRenewal = strategy.resolveAutoRenewal(request.getAutoRenewal());
    boolean reinvestInterest = strategy.resolveReinvestInterest(request.getReinvestInterest());

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
   * Moves money from a user's account into an active deposit.
   *
   * @param userId expected owner of the source account and deposit
   * @param sourceAccountId account to debit
   * @param depositId active deposit to credit
   * @param amount positive top-up amount
   * @return {@code true} when all updates are committed
   * @throws IllegalArgumentException when ownership, product rules, currency, amount, or balance checks fail
   */
  public boolean topUpDeposit(Long userId, Long sourceAccountId, Long depositId, BigDecimal amount) {
    validatePositive(amount, "Сумма пополнения должна быть больше нуля");

    return transactionRunner.run("Не удалось пополнить депозит", connection -> {
      Account sourceAccount = getAccountForUpdate(connection, sourceAccountId, "Выберите счет списания");
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException("Депозит не найден"));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);

      validateAccountBelongsToUser(sourceAccount, userId);
      validateDepositBelongsToUser(deposit, userId);
      validateActiveDeposit(deposit);
      validateTopUpAllowed(strategy);
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
   * Withdraws part of an active deposit to a user's account when the product allows it.
   *
   * @param userId expected owner of the deposit and target account
   * @param depositId deposit to debit
   * @param targetAccountId account to credit
   * @param amount positive withdrawal amount
   * @return {@code true} when all updates are committed
   * @throws IllegalArgumentException when ownership, product rules, currency, amount, or balance checks fail
   */
  public boolean withdrawFromDeposit(Long userId, Long depositId, Long targetAccountId, BigDecimal amount) {
    validatePositive(amount, "Сумма снятия должна быть больше нуля");

    return transactionRunner.run("Не удалось снять деньги с депозита", connection -> {
      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException("Депозит не найден"));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      Account targetAccount = getAccountForUpdate(connection, targetAccountId, "Выберите счет зачисления");

      validateDepositBelongsToUser(deposit, userId);
      validateAccountBelongsToUser(targetAccount, userId);
      validateActiveDeposit(deposit);
      validateWithdrawalAllowed(strategy, depositType);
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
   * Loads deposits waiting for manager approval.
   *
   * @return pending deposit records
   */
  public List<Deposit> getPendingDeposits() {
    return depositDao.getPendingDeposits();
  }

  /**
   * Approves a pending deposit.
   *
   * @param depositId deposit request identifier
   * @return {@code true} when the DAO changes the status to active
   */
  public boolean approveDeposit(Long depositId) {
    return depositDao.acceptDeposit(depositId);
  }

  /**
   * Rejects a pending deposit.
   *
   * @param depositId deposit request identifier
   * @return {@code true} when the DAO changes the status to rejected
   */
  public boolean rejectDeposit(Long depositId) {
    return depositDao.setStatus(depositId, DepositStatus.REJECTED);
  }

  /**
   * Calculates monthly rewards for active deposits and records reward transactions.
   *
   * @return number of active deposits for which positive interest was processed
   * @throws IllegalArgumentException when a referenced deposit type is missing
   */
  public int accrueInterestForActiveDeposits() {
    int updated = 0;

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      BigDecimal monthlyInterest = strategy.calculateMonthlyInterest(deposit, depositType);

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
   * Closes or renews active deposits whose configured maturity date has passed.
   *
   * @return number of deposits whose maturity was processed
   * @throws IllegalArgumentException when a referenced deposit type is missing
   */
  public int processExpiredDeposits() {
    int updated = 0;
    LocalDate today = LocalDate.now();

    for (Deposit deposit : depositDao.getDepositsByStatus(DepositStatus.ACTIVE)) {
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      DepositProductStrategy strategy = strategyResolver.resolve(depositType);
      LocalDate endDate = strategy.maturityDate(deposit, depositType);
      if (endDate == null) {
        continue;
      }
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

  /**
   * Checks whether the current deposit state and product rules allow top-up.
   *
   * @param deposit deposit instance to inspect
   * @param depositType product configuration for the deposit
   * @return {@code true} when the deposit is active and the strategy allows top-up
   */
  public boolean canTopUpDeposit(Deposit deposit, DepositType depositType) {
    return DepositStatus.ACTIVE.name().equals(deposit.getStatus()) && strategyResolver.resolve(depositType).canTopUp();
  }

  /**
   * Checks whether a product configuration allows partial withdrawals.
   *
   * @param depositType product configuration
   * @return {@code true} when the product strategy allows withdrawals
   */
  public boolean canWithdrawDeposit(DepositType depositType) {
    return strategyResolver.resolve(depositType).canWithdraw(depositType);
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

  private void validateTopUpAllowed(DepositProductStrategy strategy) {
    if (!strategy.canTopUp()) {
      throw new IllegalArgumentException("Депозит Капитал нельзя пополнять");
    }
  }

  private void validateWithdrawalAllowed(DepositProductStrategy strategy, DepositType depositType) {
    if (!strategy.canWithdraw(depositType)) {
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
