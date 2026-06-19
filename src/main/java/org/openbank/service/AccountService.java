package org.openbank.service;

import org.openbank.dao.account.AccountDao;
import org.openbank.dao.currency.CurrencyDao;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.status.AccountStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Applies account lifecycle rules before account data is created or changed.
 *
 * <p>The service validates ownership, status transitions, balances, transaction limits, and
 * generated card data so controllers can delegate account operations to one consistent place.</p>
 */
@Service
public class AccountService {

  private static final int MAX_ACTIVE_ACCOUNTS = 10;

  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final BankCardGenerator bankCardGenerator;
  public AccountService(AccountDao accountDao, CurrencyDao currencyDao, BankCardGenerator bankCardGenerator) {
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.bankCardGenerator = bankCardGenerator;
  }

  /**
   * Creates a pending account request for a client.
   *
   * @param userId owner of the new account
   * @param currencyName requested account currency
   * @param transactionLimit maximum amount allowed for a single account operation
   * @param name user-visible account name
   * @return {@code true} when the request is stored
   * @throws IllegalArgumentException when required input is missing or invalid
   * @throws BankDataAccessException when the requested currency cannot be found
   */
  public boolean createNewAccount(Long userId, String currencyName, BigDecimal transactionLimit, String name) {
    validateText(name, Messages.get("validation.accountName.required"));
    validateText(currencyName, Messages.get("validation.currency.required"));
    validatePositiveOrZero(transactionLimit, Messages.get("account.validation.limit.negative"));

    Currency currency = currencyDao.getCurrencyByName(currencyName).orElseThrow(() -> new BankDataAccessException(Messages.get("error.currency.notFound")));

    String cardNumber = generateUniqueCardNumber();
    String cvv = bankCardGenerator.generateCvv();
    LocalDate expiryDate = bankCardGenerator.generateExpiryDate();

    return accountDao.createNewAccount(
        userId,
        cardNumber,
        cvv,
        expiryDate,
        BigDecimal.ZERO,
        currency.getCurrencyId(),
        AccountStatus.PENDING,
        transactionLimit,
        name,
        false
    );
  }

  /**
   * Returns all currencies available for opening accounts.
   *
   * @return configured currencies from the database
   */
  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  /**
   * Loads all accounts owned by a client.
   *
   * @param userId owner identifier
   * @return accounts belonging to the user, including non-active records returned by the DAO
   */
  public List<Account> getAccountsByUserId(Long userId) {
    return accountDao.getAccountsByUserId(userId);
  }

  /**
   * Loads account requests waiting for manager approval.
   *
   * @return pending account records
   */
  public List<Account> getPendingAccounts() {
    return accountDao.getAccountsByStatus(AccountStatus.PENDING);
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
   * Withdraws money from an account after checking balance and transaction limit.
   *
   * @param accountId account to debit
   * @param amountToWithdraw amount to withdraw
   * @return {@code true} when validation passes and the DAO update is called
   * @throws IllegalArgumentException when the account is missing, amount is invalid, funds are
   *     insufficient, or the limit is exceeded
   */
  public boolean withdraw(Long accountId, BigDecimal amountToWithdraw) {
    Account account = checkAccountExists(accountId);

    validatePositiveOrZero(amountToWithdraw, Messages.get("account.validation.withdraw.negative"));
    validatePositiveOrZero(account.getBalance().subtract(amountToWithdraw), Messages.get("account.validation.insufficientFunds"));
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToWithdraw), Messages.get("account.validation.withdraw.limitExceeded"));
    accountDao.withdraw(accountId, amountToWithdraw);

    return true;
  }

  /**
   * Changes the per-operation limit for an active account owned by the current user.
   *
   * @param userId expected account owner
   * @param accountId account to update
   * @param transactionLimit new non-negative limit
   * @return {@code true} when the DAO updates the limit
   * @throws IllegalArgumentException when ownership, status, or limit validation fails
   */
  public boolean updateTransactionLimit(Long userId, Long accountId, BigDecimal transactionLimit) {
    validatePositiveOrZero(transactionLimit, Messages.get("account.validation.limit.negative"));
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(Messages.get("account.validation.limit.activeOnly"));
    }

    return accountDao.updateTransactionLimit(accountId, transactionLimit);
  }

  /**
   * Deactivates an active account owned by a user.
   *
   * @param userId expected account owner
   * @param accountId account to deactivate
   * @return {@code true} when the status was changed
   * @throws IllegalArgumentException when the account is missing, not owned by the user, or not active
   */
  public boolean deactivateAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(Messages.get("account.validation.deactivate.activeOnly"));
    }

    boolean updated = accountDao.setStatusToAccount(accountId, AccountStatus.DEACTIVATED);
    if (updated && Boolean.TRUE.equals(account.getMain())) {
      accountDao.clearMainAccount(userId);
    }

    return updated;
  }

  /**
   * Marks one active account as the user's main account.
   *
   * @param userId expected account owner
   * @param accountId account that should become main
   * @return {@code true} when the main-account flag is updated
   * @throws IllegalArgumentException when the account is missing, belongs to another user, or is not active
   */
  public boolean makeMainAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(Messages.get("account.validation.main.activeOnly"));
    }

    accountDao.clearMainAccount(userId);
    return accountDao.setMainAccount(accountId);
  }

  /**
   * Approves a pending account request and promotes the first active account to main automatically.
   *
   * @param accountId pending account identifier
   * @return {@code true} when the status was changed to active
   * @throws IllegalArgumentException when the request is not pending or the user already has the
   *     maximum number of active accounts
   */
  public boolean approveAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(Messages.get("account.validation.approve.pendingOnly"));
    }

    long activeAccounts = accountDao.countAccountsByUserIdAndStatus(account.getUserId(), AccountStatus.ACTIVE);
    if (activeAccounts >= MAX_ACTIVE_ACCOUNTS) {
      accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
      throw new IllegalArgumentException(Messages.get("account.validation.maxActiveAccounts"));
    }

    boolean approved = accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.ACTIVE);
    if (approved && activeAccounts == 0) {
      accountDao.clearMainAccount(account.getUserId());
      accountDao.setMainAccount(accountId);
    }

    return approved;
  }

  /**
   * Rejects a pending account request.
   *
   * @param accountId pending account identifier
   * @return {@code true} when the status was changed to rejected
   * @throws IllegalArgumentException when the account is missing or not pending
   */
  public boolean rejectAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(Messages.get("account.validation.reject.pendingOnly"));
    }

    return accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  /**
   * Adds money to an account after checking the account operation limit.
   *
   * @param accountId account to credit
   * @param amountToTopUp amount to add
   * @return {@code true} when validation passes and the DAO update is called
   * @throws IllegalArgumentException when the account is missing, amount is invalid, or the limit is exceeded
   */
  public boolean topUp(Long accountId, BigDecimal amountToTopUp) {
    Account account = checkAccountExists(accountId);

    validatePositiveOrZero(amountToTopUp, Messages.get("account.validation.withdraw.negative"));
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToTopUp), Messages.get("account.validation.topUp.limitExceeded"));
    accountDao.topUp(accountId, amountToTopUp);

    return true;
  }

  private String generateUniqueCardNumber() {
    String cardNumber;

    do {
      cardNumber = bankCardGenerator.generateCardNumber();
    } while (accountDao.getAccountByCardNumber(cardNumber).isPresent());

    return cardNumber;
  }

  private void validatePositiveOrZero(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(message);
    }
  }

  private void validateText(String value, String message) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(message);
    }
  }

  private Account checkAccountExists(Long accountId) {
    Optional<Account> account = accountDao.getAccountById(accountId);
    if (account.isEmpty()) {
      throw new IllegalArgumentException(Messages.get("error.account.notFound"));
    }
    return account.get();
  }

  private void validateAccountBelongsToUser(Account account, Long userId) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException(Messages.get("account.validation.notOwner"));
    }
  }

}
