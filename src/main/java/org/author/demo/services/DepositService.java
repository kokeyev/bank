package org.author.demo.services;

import org.author.demo.dao.account.AccountDao;
import org.author.demo.dao.currency.CurrencyDao;
import org.author.demo.dao.deposit.DepositDao;
import org.author.demo.dao.depositType.DepositTypeDao;
import org.author.demo.dao.transaction.TransactionDao;
import org.author.demo.db.ConnectionPool;
import org.author.demo.dto.OpenDepositRequest;
import org.author.demo.model.Account;
import org.author.demo.model.Deposit;
import org.author.demo.model.DepositType;
import org.author.demo.model.status.DepositStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
  private final ConnectionPool connectionPool;

  public DepositService(DepositDao depositDao, DepositTypeDao depositTypeDao, AccountDao accountDao, CurrencyDao currencyDao, TransactionDao transactionDao, ConnectionPool connectionPool) {
    this.depositDao = depositDao;
    this.depositTypeDao = depositTypeDao;
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.transactionDao = transactionDao;
    this.connectionPool = connectionPool;
  }

  public List<DepositType> getDepositTypesByProduct(String productName) {
    return depositTypeDao.getAllDepositTypes()
        .stream()
        .filter(type -> type.getName().equals(productName))
        .toList();
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
    validatePositive(request.getAmount(), "Сумма депозита должна быть больше нуля");

    DepositType depositType = depositTypeDao.getDepositTypeById(request.getDepositTypeId())
        .orElseThrow(() -> new IllegalArgumentException("Выберите условия депозита"));

    validateDepositAmount(depositType, request.getAmount());

    boolean autoRenewal = resolveAutoRenewal(depositType.getName(), request.getAutoRenewal());
    boolean reinvestInterest = resolveReinvestInterest(depositType.getName(), request.getReinvestInterest());

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account sourceAccount = getAccountByIdForUpdate(connection, request.getSourceAccountId());
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

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось открыть депозит", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  public boolean topUpDeposit(Long userId, Long sourceAccountId, Long depositId, BigDecimal amount) {
    validatePositive(amount, "Сумма пополнения должна быть больше нуля");

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account sourceAccount = getAccountByIdForUpdate(connection, sourceAccountId);
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

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось пополнить депозит", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  public boolean withdrawFromDeposit(Long userId, Long depositId, Long targetAccountId, BigDecimal amount) {
    validatePositive(amount, "Сумма снятия должна быть больше нуля");

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Deposit deposit = depositDao.getDepositByIdForUpdate(connection, depositId)
          .orElseThrow(() -> new IllegalArgumentException("Депозит не найден"));
      DepositType depositType = depositTypeDao.getDepositTypeById(deposit.getDepositTypeId())
          .orElseThrow(() -> new IllegalArgumentException("Тип депозита не найден"));
      Account targetAccount = getAccountByIdForUpdate(connection, targetAccountId);

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

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось снять деньги с депозита", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  public List<Deposit> getPendingDeposits() {
    return depositDao.getPendingDeposits();
  }

  public boolean approveDeposit(Long depositId) {
    return depositDao.acceptDeposit(depositId);
  }

  public boolean rejectDeposit(Long depositId) {
    return depositDao.setStatus(depositId, DepositStatus.REJECTED);
  }

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

      Connection connection = null;
      try {
        connection = connectionPool.getConnection();
        connection.setAutoCommit(false);

        if (Boolean.TRUE.equals(deposit.getReinvestInterest())) {
          depositDao.topUpDeposit(connection, deposit.getDepositId(), monthlyInterest);
        }
        createTransactionHistory(connection, null, null, monthlyInterest, depositType.getCurrencyId(), "Начисление вознаграждения по депозиту #" + deposit.getDepositId(), DEPOSIT_INTEREST);

        connection.commit();
        updated++;
      } catch (SQLException | RuntimeException e) {
        rollback(connection);
        if (e instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }
        throw new RuntimeException("Не удалось начислить вознаграждение", e);
      } finally {
        resetAutoCommit(connection);
        connectionPool.releaseConnection(connection);
      }
    }

    return updated;
  }

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

      Connection connection = null;
      try {
        connection = connectionPool.getConnection();
        connection.setAutoCommit(false);

        if (Boolean.TRUE.equals(deposit.getAutoRenewal())) {
          depositDao.updateStartDate(connection, deposit.getDepositId(), today);
        } else {
          depositDao.setStatus(connection, deposit.getDepositId(), DepositStatus.EXPIRED);
        }

        connection.commit();
        updated++;
      } catch (SQLException | RuntimeException e) {
        rollback(connection);
        if (e instanceof RuntimeException runtimeException) {
          throw runtimeException;
        }
        throw new RuntimeException("Не удалось обработать срок депозита", e);
      } finally {
        resetAutoCommit(connection);
        connectionPool.releaseConnection(connection);
      }
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

  private Account getAccountByIdForUpdate(Connection connection, Long accountId) throws SQLException {
    if (accountId == null) {
      throw new IllegalArgumentException("Выберите счет списания");
    }

    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where account_id = ?
        for update
        """;

    try (var statement = connection.prepareStatement(sql)) {
      statement.setLong(1, accountId);
      try (var resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          var expiryDate = resultSet.getDate("expiry_date");

          return new Account(
              resultSet.getLong("account_id"),
              resultSet.getLong("user_id"),
              resultSet.getString("card_number"),
              resultSet.getString("cvv"),
              expiryDate == null ? null : expiryDate.toLocalDate(),
              resultSet.getBigDecimal("balance"),
              resultSet.getLong("currency_id"),
              resultSet.getString("status"),
              resultSet.getBigDecimal("transaction_limit"),
              resultSet.getString("name"),
              resultSet.getBoolean("is_main")
          );
        }
      }
    }

    throw new IllegalArgumentException("Счет не найден");
  }

  private void withdrawFromAccount(Connection connection, Long accountId, BigDecimal amount) throws SQLException {
    String sql = """
        update accounts
        set balance = balance - ?
        where account_id = ?
        """;

    try (var statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, accountId);

      if (statement.executeUpdate() == 0) {
        throw new IllegalStateException("Не удалось списать деньги со счета");
      }
    }
  }

  private void topUpAccount(Connection connection, Long accountId, BigDecimal amount) throws SQLException {
    String sql = """
        update accounts
        set balance = balance + ?
        where account_id = ?
        """;

    try (var statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, accountId);

      if (statement.executeUpdate() == 0) {
        throw new IllegalStateException("Не удалось зачислить деньги на счет");
      }
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

  private void rollback(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.rollback();
    } catch (SQLException ignored) {
    }
  }

  private void resetAutoCommit(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.setAutoCommit(true);
    } catch (SQLException ignored) {
    }
  }
}
