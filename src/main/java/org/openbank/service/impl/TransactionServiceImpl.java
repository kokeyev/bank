package org.openbank.service.impl;

import org.openbank.service.BankSettingsService;
import org.openbank.service.DatabaseTransactionRunner;
import org.openbank.service.MessageService;
import org.openbank.service.TransactionService;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.dao.LoanDao;
import org.openbank.dao.TransactionDao;
import org.openbank.dao.UserDao;
import org.openbank.model.Account;
import org.openbank.model.Loan;
import org.openbank.model.Transaction;
import org.openbank.model.User;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.model.status.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionServiceImpl implements TransactionService {

  private final AccountDao accountDao;
  private final TransactionDao transactionDao;
  private final CurrencyDao currencyDao;
  private final UserDao userDao;
  private final LoanDao loanDao;
  private final BankSettingsService bankSettingsService;
  private final DatabaseTransactionRunner transactionRunner;
  private final MessageService messageService;

  public TransactionServiceImpl(AccountDao accountDao, TransactionDao transactionDao, CurrencyDao currencyDao, UserDao userDao, LoanDao loanDao, BankSettingsService bankSettingsService, DatabaseTransactionRunner transactionRunner, MessageService messageService) {
    this.accountDao = accountDao;
    this.transactionDao = transactionDao;
    this.currencyDao = currencyDao;
    this.userDao = userDao;
    this.loanDao = loanDao;
    this.bankSettingsService = bankSettingsService;
    this.transactionRunner = transactionRunner;
    this.messageService = messageService;
  }

  public boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    validatePositive(amount, messageService.get("transaction.validation.amount.positive"));

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

  public List<Transaction> getTransactionsByUserId(Long userId, int limit, int offset) {
    return transactionDao.getTransactionsByUserId(userId, limit, offset);
  }

  public int countTransactionsByUserId(Long userId) {
    return transactionDao.countTransactionsByUserId(userId);
  }

  public boolean makeTransactionBetweenAccountsOfOneClient(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    Account senderAccount = checkAccountExists(senderAccountId);
    Account receiverAccount = checkAccountExists(receiverAccountId);

    if (!senderAccount.getUserId().equals(receiverAccount.getUserId())) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.accounts.differentOwners"));
    }

    return makeTransactionBetweenAccountsOfOneClient(senderAccount.getUserId(), senderAccountId, receiverAccountId, amount, "");
  }

  public boolean makeTransactionBetweenAccountsOfOneClient(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message) {
    validateTransferRequest(userId, senderAccountId, receiverAccountId, amount);

    return transactionRunner.run(messageService.get("transfers.error"), connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountForUpdate(connection, receiverAccountId);

      validateAccountBelongsToUser(senderAccount, userId, messageService.get("transaction.validation.senderAccount.notOwner"));
      validateAccountBelongsToUser(receiverAccount, userId, messageService.get("transaction.validation.receiverAccount.notOwner"));
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, messageService.get("transaction.validation.transferLimit.exceeded"));
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, TransactionType.BETWEEN_OWN_ACCOUNTS.name());

      return true;
    });
  }

  public boolean makeTransactionByPhoneNumber(Long senderAccountId, String receiverPhoneNumber, BigDecimal amount) {
    validateText(receiverPhoneNumber, messageService.get("validation.recipientPhone.required"));

    Optional<User> userOptional = userDao.getUserByPhoneNumber(cleanPhone(receiverPhoneNumber));
    if (userOptional.isEmpty()) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.receiver.notFound"));
    }
    User receiver = userOptional.get();

    Optional<Account> accountOptional = accountDao.getMainActiveAccountByUserId(receiver.getUserId());
    if (accountOptional.isEmpty()) {
      accountOptional = accountDao.getFirstActiveAccountByUserId(receiver.getUserId());
    }
    if (accountOptional.isEmpty()) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.receiver.noActiveAccount"));
    }
    Account receiverAccount = accountOptional.get();

    return transferToAccount(senderAccountId, receiverAccount.getAccountId(), amount, messageService.get("transaction.message.phoneTransfer", receiverPhoneNumber), TransactionType.PHONE_TRANSFER.name());
  }

  public boolean makeTransactionByCardNumber(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validateText(receiverCardNumber, messageService.get("validation.cardNumber.required"));
    String cleanedCard = cleanCard(receiverCardNumber);
    Optional<Account> receiverAccount = accountDao.getAccountByCardNumber(cleanedCard);

    if (receiverAccount.isPresent()) {
      return transferToAccount(senderAccountId, receiverAccount.get().getAccountId(), amount, messageService.get("transaction.message.cardTransfer", cleanedCard), TransactionType.CARD_TRANSFER.name());
    }

    return transferToExternalCard(senderAccountId, cleanedCard, amount);
  }

  public boolean makeTransactionTopUpLoan(Long senderAccountId, Long loanId, BigDecimal amount) {
    validatePositive(amount, messageService.get("payment.validation.amount.positive"));

    return transactionRunner.run(messageService.get("transfers.loan.error"), connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      Loan loan = loanDao.getLoanById(loanId).orElseThrow(() -> new IllegalArgumentException(messageService.get("error.loan.notFound")));

      validateActive(senderAccount);
      validateAccountBelongsToUser(senderAccount, loan.getUserId(), messageService.get("transaction.validation.senderAccount.notLoanOwner"));
      if (!LoanStatus.ACTIVE.name().equals(loan.getStatus())) {
        throw new IllegalArgumentException(messageService.get("loan.validation.payment.activeOnly"));
      }

      validateTransferFrom(senderAccount, amount, messageService.get("transaction.validation.paymentLimit.exceeded"));

      BigDecimal kztAmount = convert(amount, senderAccount.getCurrencyId(), getKztCurrencyId());
      withdraw(connection, senderAccountId, amount);
      payLoan(connection, loanId, kztAmount);
      createTransactionHistory(connection, senderAccountId, null, amount, senderAccount.getCurrencyId(), messageService.get("transaction.message.loanPayment", loanId), TransactionType.LOAN_PAYMENT.name());

      return true;
    });
  }

  public boolean makeTransactionExchangeCurrencies(Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    return transferToAccount(senderAccountId, receiverAccountId, amount, messageService.get("transaction.message.currencyExchange"), TransactionType.CURRENCY_EXCHANGE.name());
  }

  public boolean topUpAccount(Long userId, Long accountId, BigDecimal amount) {
    validatePositive(amount, messageService.get("topUp.validation.amount.positive"));
    if (userId == null) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.loginRequired.topUp"));
    }
    if (accountId == null) {
      throw new IllegalArgumentException(messageService.get("validation.topUpAccount.required"));
    }

    return transactionRunner.run(messageService.get("transfers.accountTopUp.error"), connection -> {
      Account account = getAccountForUpdate(connection, accountId);
      validateAccountBelongsToUser(account, userId, messageService.get("account.validation.notOwner"));
      validateActive(account);

      topUp(connection, accountId, amount);
      createTransactionHistory(connection, null, accountId, amount, account.getCurrencyId(), messageService.get("transaction.message.accountTopUp"), TransactionType.ACCOUNT_TOP_UP.name());

      return true;
    });
  }

  private boolean transferToAccount(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message, String transactionType) {
    validatePositive(amount, messageService.get("transfer.validation.amount.positive"));
    if (senderAccountId == null) {
      throw new IllegalArgumentException(messageService.get("validation.senderAccount.required"));
    }
    if (receiverAccountId == null) {
      throw new IllegalArgumentException(messageService.get("validation.receiverAccount.required"));
    }
    if (senderAccountId.equals(receiverAccountId)) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.accounts.different"));
    }

    return transactionRunner.run(messageService.get("transfers.error"), connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      Account receiverAccount = getAccountForUpdate(connection, receiverAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, messageService.get("transaction.validation.transferLimit.exceeded"));
      validateActive(receiverAccount);

      withdraw(connection, senderAccountId, debitAmount);
      topUp(connection, receiverAccountId, convert(amount, senderAccount.getCurrencyId(), receiverAccount.getCurrencyId()));
      createTransactionHistory(connection, senderAccountId, receiverAccountId, amount, senderAccount.getCurrencyId(), fee, message, transactionType);

      return true;
    });
  }

  private boolean transferToExternalCard(Long senderAccountId, String receiverCardNumber, BigDecimal amount) {
    validatePositive(amount, messageService.get("transfer.validation.amount.positive"));

    return transactionRunner.run(messageService.get("transaction.operation.externalCardTransfer.error"), connection -> {
      Account senderAccount = getAccountForUpdate(connection, senderAccountId);
      BigDecimal fee = bankSettingsService.calculateTransferFee(amount);
      BigDecimal debitAmount = amount.add(fee);
      validateTransferFrom(senderAccount, debitAmount, messageService.get("transaction.validation.transferLimit.exceeded"));

      withdraw(connection, senderAccountId, debitAmount);
      createTransactionHistory(connection, senderAccountId, null, amount, senderAccount.getCurrencyId(), fee, messageService.get("transaction.message.externalCardTransfer", receiverCardNumber), TransactionType.EXTERNAL_CARD_TRANSFER.name());

      return true;
    });
  }

  private void validateTransferRequest(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount) {
    if (userId == null) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.loginRequired.transfer"));
    }
    if (senderAccountId == null) {
      throw new IllegalArgumentException(messageService.get("validation.senderAccount.required"));
    }
    if (receiverAccountId == null) {
      throw new IllegalArgumentException(messageService.get("validation.receiverAccount.required"));
    }
    if (senderAccountId.equals(receiverAccountId)) {
      throw new IllegalArgumentException(messageService.get("transaction.validation.accounts.different"));
    }
    validatePositive(amount, messageService.get("transfer.validation.amount.positive"));
  }

  private Account checkAccountExists(Long accountId) {
    Optional<Account> account = accountDao.getAccountById(accountId);
    if (account.isEmpty()) {
      throw new IllegalArgumentException(messageService.get("error.account.notFound"));
    }

    return account.get();
  }

  private void validateTransferFrom(Account account, BigDecimal amount, String limitMessage) {
    validateActive(account);
    validatePositiveOrZero(account.getBalance().subtract(amount), messageService.get("account.validation.insufficientFunds"));
    validatePositiveOrZero(account.getTransactionLimit().subtract(amount), limitMessage);
  }

  private void validateActive(Account account) {
    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(messageService.get("account.validation.operations.activeOnly"));
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
    return accountDao.getAccountByIdForUpdate(connection, accountId).orElseThrow(() -> new IllegalArgumentException(messageService.get("error.account.notFound")));
  }

  private void withdraw(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.withdraw(connection, accountId, amount)) {
      throw new IllegalStateException(messageService.get("account.operation.withdraw.error"));
    }
  }

  private void topUp(Connection connection, Long accountId, BigDecimal amount) {
    if (!accountDao.topUp(connection, accountId, amount)) {
      throw new IllegalStateException(messageService.get("account.operation.topUp.error"));
    }
  }

  private void payLoan(Connection connection, Long loanId, BigDecimal amount) {
    if (!loanDao.payLoan(connection, loanId, amount)) {
      throw new IllegalStateException(messageService.get("loan.operation.update.error"));
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
      throw new IllegalStateException(messageService.get("transaction.operation.history.error"));
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
    return currencyDao.getCurrencyByName("KZT").orElseThrow(() -> new IllegalStateException(messageService.get("error.currency.kzt.notFound"))).getCurrencyId();
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
