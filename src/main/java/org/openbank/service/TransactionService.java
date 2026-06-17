package org.openbank.service;

import org.openbank.dao.account.AccountDao;
import org.openbank.dao.currency.CurrencyDao;
import org.openbank.dao.loan.LoanDao;
import org.openbank.dao.transaction.TransactionDao;
import org.openbank.dao.user.UserDao;
import org.openbank.model.Account;
import org.openbank.model.Loan;
import org.openbank.model.Transaction;
import org.openbank.model.User;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.LoanStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Provides transaction service operations.
 */
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
  private final DatabaseTransactionRunner transactionRunner;

  /**
   * Handles transaction service.
   */
  public TransactionService(AccountDao accountDao, TransactionDao transactionDao, CurrencyDao currencyDao, UserDao userDao, LoanDao loanDao, BankSettingsService bankSettingsService, DatabaseTransactionRunner transactionRunner) {
    this.accountDao = accountDao;
    this.transactionDao = transactionDao;
    this.currencyDao = currencyDao;
    this.userDao = userDao;
    this.loanDao = loanDao;
    this.bankSettingsService = bankSettingsService;
    this.transactionRunner = transactionRunner;
  }

  /**
   * Handles create new transaction.
   */
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

  /**
   * Handles get transactions by account id.
   */
  public List<Transaction> getTransactionsByAccountId(Long accountId) {
    return transactionDao.getTransactionsByAccountId(accountId);
  }

  /**
   * Handles get recent transactions by user id.
   */
  public List<Transaction> getRecentTransactionsByUserId(Long userId, int limit) {
    return transactionDao.getRecentTransactionsByUserId(userId, limit);
  }

  /**
   * Handles get transactions by user id.
   */
  public List<Transaction> getTransactionsByUserId(Long userId, int limit, int offset) {
    return transactionDao.getTransactionsByUserId(userId, limit, offset);
  }

  /**
   * Handles count transactions by user id.
   */
  public int countTransactionsByUserId(Long userId) {
    return transactionDao.countTransactionsByUserId(userId);
  }

  /**
   * Handles make transaction between accounts of one client.
   */
  public boolean makeTransactionBetweenAccountsOfOneClient(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    Account senderAccount = checkAccountExists(senderAccountId);
    Account receiverAccount = checkAccountExists(receiverAccountId);

    if (!senderAccount.getUserId().equals(receiverAccount.getUserId())) {
      throw new IllegalArgumentException("Счета принадлежат разным пользователям");
    }

    return makeTransactionBetweenAccountsOfOneClient(senderAccount.getUserId(), senderAccountId, receiverAccountId, amount, "");
  }

  /**
   * Handles make transaction between accounts of one client.
   */
  public boolean makeTransactionBetweenAccountsOfOneClient(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message) {
    validateTransferRequest(userId, senderAccountId, receiverAccountId, amount);

    return transactionRunner.run("Не удалось выполнить перевод", connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountForUpdate(connection, receiverAccountId);

      validateAccountBelongsToUser(senderAccount, userId, "Счет списания не принадлежит текущему пользователю");
      validateAccountBelongsToUser(receiverAccount, userId, "Счет зачисления не принадлежит текущему пользователю");
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, BETWEEN_OWN_ACCOUNTS);

      return true;
    });
  }

  /**
   * Handles make transaction by phone number.
   */
  public boolean makeTransactionByPhoneNumber(Long senderAccountId, String receiverPhoneNumber, BigDecimal amount) {
    validateText(receiverPhoneNumber, "Введите номер телефона получателя");

    Optional<User> userOptional = userDao.getUserByPhoneNumber(cleanPhone(receiverPhoneNumber));
    if (!userOptional.isPresent()) {
      throw new IllegalArgumentException("Получатель не найден");
    }
    User receiver = userOptional.get();

    Optional<Account> accountOptional = accountDao.getMainActiveAccountByUserId(receiver.getUserId());
    if (!accountOptional.isPresent()) {
      accountOptional = accountDao.getFirstActiveAccountByUserId(receiver.getUserId());
    }
    if (!accountOptional.isPresent()) {
      throw new IllegalArgumentException("У получателя нет активного счета");
    }
    Account receiverAccount = accountOptional.get();

    return transferToAccount(senderAccountId, receiverAccount.getAccountId(), amount, "Перевод по телефону " + receiverPhoneNumber, PHONE_TRANSFER);
  }

  /**
   * Handles make transaction by card number.
   */
  public boolean makeTransactionByCardNumber(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validateText(receiverCardNumber, "Введите номер карты");
    String cleanedCard = cleanCard(receiverCardNumber);
    Optional<Account> receiverAccount = accountDao.getAccountByCardNumber(cleanedCard);

    if (receiverAccount.isPresent()) {
      return transferToAccount(senderAccountId, receiverAccount.get().getAccountId(), amount, "Перевод на карту " + cleanedCard, CARD_TRANSFER);
    }

    return transferToExternalCard(senderAccountId, cleanedCard, amount);
  }

  /**
   * Handles make transaction top up deposit.
   */
  public boolean makeTransactionTopUpDeposit(Long senderAccountId, Long depositId, BigDecimal amount) {
    return false;
  }

  /**
   * Handles make transaction top up loan.
   */
  public boolean makeTransactionTopUpLoan(Long senderAccountId, Long loanId, BigDecimal amount) {
    validatePositive(amount, "Сумма платежа должна быть больше нуля");

    return transactionRunner.run("Не удалось погасить кредит", connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
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

      return true;
    });
  }

  /**
   * Handles make transaction exchange currencies.
   */
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

    return transactionRunner.run("Не удалось выполнить перевод", connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountForUpdate(connection, receiverAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, transactionType);

      return true;
    });
  }

  private boolean transferToExternalCard(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validatePositive(amount, "Сумма перевода должна быть больше нуля");

    return transactionRunner.run("Не удалось выполнить перевод на карту", connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, "Превышен лимит перевода");

      withdraw(connection, senderAccountId, debitAmount);
      createTransactionHistory(connection, senderAccountId, null, amount, senderAccount.getCurrencyId(), fee, "Перевод на внешнюю карту " + receiverCardNumber, EXTERNAL_CARD_TRANSFER);

      return true;
    });
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

  private Account getAccountForUpdate(Connection connection, Long accountId) {
    return accountDao.getAccountByIdForUpdate(connection, accountId)
        .orElseThrow(() -> new IllegalArgumentException("Счет не найден"));
  }

  private void withdraw(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.withdraw(connection, accountId, amount)) {
      throw new IllegalStateException("Не удалось списать деньги со счета");
    }
  }

  private void topUp(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.topUp(connection, accountId, amount)) {
      throw new IllegalStateException("Не удалось зачислить деньги на счет");
    }
  }

  private void payLoan(Connection connection, Long loanId, BigDecimal amount) {
    if (!loanDao.payLoan(connection, loanId, amount)) {
      throw new IllegalStateException("Не удалось обновить кредит");
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

}
