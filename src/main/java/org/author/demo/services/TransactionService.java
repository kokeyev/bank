package org.author.demo.services;

import org.author.demo.dao.account.AccountDao;
import org.author.demo.dao.currency.CurrencyDao;
import org.author.demo.dao.loan.LoanDao;
import org.author.demo.dao.transaction.TransactionDao;
import org.author.demo.dao.user.UserDao;
import org.author.demo.db.ConnectionPool;
import org.author.demo.model.Account;
import org.author.demo.model.Loan;
import org.author.demo.model.Transaction;
import org.author.demo.model.User;
import org.author.demo.model.status.AccountStatus;
import org.author.demo.model.status.LoanStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService {

  private static final String BETWEEN_OWN_ACCOUNTS = "BETWEEN_OWN_ACCOUNTS";
  private static final String PHONE_TRANSFER = "PHONE_TRANSFER";
  private static final String CARD_TRANSFER = "CARD_TRANSFER";
  private static final String EXTERNAL_CARD_TRANSFER = "EXTERNAL_CARD_TRANSFER";
  private static final String CURRENCY_EXCHANGE = "CURRENCY_EXCHANGE";
  private static final String LOAN_PAYMENT = "LOAN_PAYMENT";

  private final AccountDao accountDao;
  private final TransactionDao transactionDao;
  private final CurrencyDao currencyDao;
  private final UserDao userDao;
  private final LoanDao loanDao;
  private final BankSettingsService bankSettingsService;
  private final ConnectionPool connectionPool;

  public TransactionService(AccountDao accountDao, TransactionDao transactionDao, CurrencyDao currencyDao, UserDao userDao, LoanDao loanDao, BankSettingsService bankSettingsService, ConnectionPool connectionPool) {
    this.accountDao = accountDao;
    this.transactionDao = transactionDao;
    this.currencyDao = currencyDao;
    this.userDao = userDao;
    this.loanDao = loanDao;
    this.bankSettingsService = bankSettingsService;
    this.connectionPool = connectionPool;
  }

  public boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    validatePositive(amount, "Сумма транзакции должна быть больше нуля");
    return transactionDao.createNewTransaction(
        senderAccountId,
        receiverAccountId,
        LocalDateTime.now(),
        amount,
        currencyId,
        fee == null ? BigDecimal.ZERO : fee,
        clean(message),
        clean(transactionType)
    );
  }

  public List<Transaction> getTransactionsByAccountId(Long accountId) {
    return transactionDao.getTransactionsByAccountId(accountId);
  }

  public List<Transaction> getRecentTransactionsByUserId(Long userId, int limit) {
    return transactionDao.getRecentTransactionsByUserId(userId, limit);
  }

  public boolean makeTransactionBetweenAccountsOfOneClient(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    Account senderAccount = checkAccountExists(senderAccountId);
    Account receiverAccount = checkAccountExists(receiverAccountId);

    if (!senderAccount.getUserId().equals(receiverAccount.getUserId())) {
      throw new IllegalArgumentException("Счета принадлежат разным пользователям");
    }

    return makeTransactionBetweenAccountsOfOneClient(senderAccount.getUserId(), senderAccountId, receiverAccountId, amount, "");
  }

  public boolean makeTransactionBetweenAccountsOfOneClient(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message) {
    validateTransferRequest(userId, senderAccountId, receiverAccountId, amount);

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account senderAccount = getAccountByIdForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountByIdForUpdate(connection, receiverAccountId);

      validateAccountBelongsToUser(senderAccount, userId, "Счет списания не принадлежит текущему пользователю");
      validateAccountBelongsToUser(receiverAccount, userId, "Счет зачисления не принадлежит текущему пользователю");
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, BETWEEN_OWN_ACCOUNTS);

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось выполнить перевод", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  public boolean makeTransactionByPhoneNumber(Long senderAccountId, String receiverPhoneNumber, BigDecimal amount) {
    validateText(receiverPhoneNumber, "Введите номер телефона получателя");
    User receiver = userDao.getUserByPhoneNumber(cleanPhone(receiverPhoneNumber))
        .orElseThrow(() -> new IllegalArgumentException("Получатель не найден"));
    Account receiverAccount = accountDao.getMainActiveAccountByUserId(receiver.getUserId())
        .or(() -> accountDao.getFirstActiveAccountByUserId(receiver.getUserId()))
        .orElseThrow(() -> new IllegalArgumentException("У получателя нет активного счета"));

    return transferToAccount(senderAccountId, receiverAccount.getAccountId(), amount, "Перевод по телефону " + receiverPhoneNumber, PHONE_TRANSFER);
  }

  public boolean makeTransactionByCardNumber(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validateText(receiverCardNumber, "Введите номер карты");
    String cleanedCard = cleanCard(receiverCardNumber);
    Optional<Account> receiverAccount = accountDao.getAccountByCardNumber(cleanedCard);

    if (receiverAccount.isPresent()) {
      return transferToAccount(senderAccountId, receiverAccount.get().getAccountId(), amount, "Перевод на карту " + cleanedCard, CARD_TRANSFER);
    }

    return transferToExternalCard(senderAccountId, cleanedCard, amount);
  }

  public boolean makeTransactionTopUpDeposit(Long senderAccountId, Long depositId, BigDecimal amount) {
    return false;
  }

  public boolean makeTransactionTopUpLoan(Long senderAccountId, Long loanId, BigDecimal amount) {
    validatePositive(amount, "Сумма платежа должна быть больше нуля");

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account senderAccount = getAccountByIdForUpdate(connection, senderAccountId);
      Loan loan = loanDao.getLoanById(loanId)
          .orElseThrow(() -> new IllegalArgumentException("Кредит не найден"));

      validateActive(senderAccount);
      validateAccountBelongsToUser(senderAccount, loan.getUserId(), "Счет списания не принадлежит владельцу кредита");
      if (!LoanStatus.ACTIVE.name().equals(loan.getStatus())) {
        throw new IllegalArgumentException("Погасить можно только активный кредит");
      }

      validateTransferFrom(senderAccount, amount, "Превышен лимит платежа");

      BigDecimal kztAmount = convert(amount, senderAccount.getCurrencyId(), getKztCurrencyId());
      withdraw(connection, senderAccountId, amount);
      payLoan(connection, loanId, kztAmount);
      createTransactionHistory(connection, senderAccountId, null, amount, senderAccount.getCurrencyId(), "Погашение кредита #" + loanId, LOAN_PAYMENT);

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось погасить кредит", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  public boolean makeTransactionExchangeCurrencies(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    return transferToAccount(senderAccountId, receiverAccountId, amount, "Обмен валют", CURRENCY_EXCHANGE);
  }

  private boolean transferToAccount(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message, String transactionType) {
    validatePositive(amount, "Сумма перевода должна быть больше нуля");
    if (senderAccountId == null) {
      throw new IllegalArgumentException("Выберите счет списания");
    }
    if (receiverAccountId == null) {
      throw new IllegalArgumentException("Выберите счет зачисления");
    }
    if (senderAccountId.equals(receiverAccountId)) {
      throw new IllegalArgumentException("Выберите разные счета");
    }

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account senderAccount = getAccountByIdForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountByIdForUpdate(connection, receiverAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, transactionType);

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось выполнить перевод", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  private boolean transferToExternalCard(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validatePositive(amount, "Сумма перевода должна быть больше нуля");

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Account senderAccount = getAccountByIdForUpdate(connection, senderAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");

      withdraw(connection, senderAccountId, debitAmount);
      createTransactionHistory(connection, senderAccountId, null, amount, senderAccount.getCurrencyId(), fee, "Перевод на внешнюю карту " + receiverCardNumber, EXTERNAL_CARD_TRANSFER);

      connection.commit();
      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new RuntimeException("Не удалось выполнить перевод на карту", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  private void validateTransferRequest(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    if (userId == null) {
      throw new IllegalArgumentException("Войдите в аккаунт, чтобы выполнить перевод");
    }
    if (senderAccountId == null) {
      throw new IllegalArgumentException("Выберите счет списания");
    }
    if (receiverAccountId == null) {
      throw new IllegalArgumentException("Выберите счет зачисления");
    }
    if (senderAccountId.equals(receiverAccountId)) {
      throw new IllegalArgumentException("Выберите разные счета");
    }
    validatePositive(amount, "Сумма перевода должна быть больше нуля");
  }

  private Account checkAccountExists(Long accountId) {
    Optional<Account> account = accountDao.getAccountById(accountId);
    if (account.isEmpty()) {
      throw new IllegalArgumentException("Счет не найден");
    }
    return account.get();
  }

  private void validateTransferFrom(Account account, BigDecimal amount, String limitMessage) {
    validateActive(account);
    validatePositiveOrZero(account.getBalance().subtract(amount), "Не хватает средств на счету");
    validatePositiveOrZero(account.getTransactionLimit().subtract(amount), limitMessage);
  }

  private void validateActive(Account account) {
    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Операции доступны только для активных счетов");
    }
  }

  private void validatePositiveOrZero(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private void validatePositive(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private void validateAccountBelongsToUser(Account account, Long userId, String message) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException(message);
    }
  }

  private Account getAccountByIdForUpdate(Connection connection, Long accountId) throws SQLException {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where account_id = ?
        for update
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, accountId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return mapAccount(resultSet);
        }
      }
    }

    throw new IllegalArgumentException("Счет не найден");
  }

  private void withdraw(Connection connection, Long accountId, BigDecimal amount) throws SQLException {
    String sql = """
        update accounts
        set balance = balance - ?
        where account_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, accountId);
      executeRequiredUpdate(statement, "Не удалось списать деньги со счета");
    }
  }

  private void topUp(Connection connection, Long accountId, BigDecimal amount) throws SQLException {
    String sql = """
        update accounts
        set balance = balance + ?
        where account_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, accountId);
      executeRequiredUpdate(statement, "Не удалось зачислить деньги на счет");
    }
  }

  private void payLoan(Connection connection, Long loanId, BigDecimal amount) throws SQLException {
    String sql = """
        update loans
        set remaining_amount = greatest(remaining_amount - ?, 0),
            status = case when remaining_amount - ? <= 0 then ? else status end
        where loan_id = ? and status = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setBigDecimal(2, amount);
      statement.setString(3, LoanStatus.CLOSED.name());
      statement.setLong(4, loanId);
      statement.setString(5, LoanStatus.ACTIVE.name());
      executeRequiredUpdate(statement, "Не удалось обновить кредит");
    }
  }

  private void createTransactionHistory(Connection connection, Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, String message, String transactionType) {
    createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, currencyId, BigDecimal.ZERO, message, transactionType);
  }

  private void createTransactionHistory(Connection connection, Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    boolean created = transactionDao.createNewTransaction(
        connection,
        senderAccountId,
        receiverAccountId,
        LocalDateTime.now(),
        amount,
        currencyId,
        fee == null ? BigDecimal.ZERO : fee,
        clean(message),
        transactionType
    );

    if (!created) {
      throw new IllegalStateException("Не удалось сохранить историю перевода");
    }
  }

  private BigDecimal convert(BigDecimal amount, Long fromCurrencyId, Long toCurrencyId) {
    if (fromCurrencyId.equals(toCurrencyId)) {
      return amount;
    }

    BigDecimal fromRate = currencyDao.getCurrencyRateToKztById(fromCurrencyId);
    BigDecimal toRate = currencyDao.getCurrencyRateToKztById(toCurrencyId);
    return amount.multiply(fromRate).divide(toRate, 2, RoundingMode.HALF_UP);
  }

  private Long getKztCurrencyId() {
    return currencyDao.getCurrencyByName("KZT")
        .orElseThrow(() -> new IllegalStateException("Валюта KZT не найдена"))
        .getCurrencyId();
  }

  private void executeRequiredUpdate(PreparedStatement statement, String message) throws SQLException {
    if (statement.executeUpdate() == 0) {
      throw new IllegalStateException(message);
    }
  }

  private Account mapAccount(ResultSet resultSet) throws SQLException {
    Date expiryDate = resultSet.getDate("expiry_date");

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

  private String cleanPhone(String phone) {
    String digits = phone == null ? "" : phone.replaceAll("[^0-9+]", "");
    if (digits.startsWith("+7")) {
      return digits;
    }
    if (digits.startsWith("8") && digits.length() == 11) {
      return "+7" + digits.substring(1);
    }
    if (digits.startsWith("7") && digits.length() == 11) {
      return "+" + digits;
    }
    if (digits.length() == 10) {
      return "+7" + digits;
    }
    return digits;
  }

  private String cleanCard(String cardNumber) {
    return cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
  }

  private void validateText(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
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
