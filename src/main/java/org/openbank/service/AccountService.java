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
 * Provides account service operations.
 */
@Service
public class AccountService {

  private static final int MAX_ACTIVE_ACCOUNTS = 10;

  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final BankCardGenerator bankCardGenerator;

  /**
   * Handles account service.
   */
  public AccountService(AccountDao accountDao, CurrencyDao currencyDao, BankCardGenerator bankCardGenerator) {
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.bankCardGenerator = bankCardGenerator;
  }

  /**
   * Handles create new account.
   */
  public boolean createNewAccount(Long userId, String currencyName, BigDecimal transactionLimit, String name) {
    validateText(name, "Введите название счета");
    validateText(currencyName, "Выберите валюту");
    validatePositiveOrZero(transactionLimit, "Лимит не может быть отрицательным");

    Currency currency = currencyDao.getCurrencyByName(currencyName).orElseThrow(() -> new BankDataAccessException("Валюта не найдена"));

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
   * Handles get all currencies.
   */
  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  /**
   * Handles get accounts by user id.
   */
  public List<Account> getAccountsByUserId(Long userId) {
    return accountDao.getAccountsByUserId(userId);
  }

  /**
   * Handles get pending accounts.
   */
  public List<Account> getPendingAccounts() {
    return accountDao.getAccountsByStatus(AccountStatus.PENDING);
  }

  /**
   * Handles get currency name by id.
   */
  public String getCurrencyNameById(Long currencyId) {
    return currencyDao.getCurrencyNameById(currencyId);
  }

  /**
   * Handles withdraw.
   */
  public boolean withdraw(Long accountId, BigDecimal amountToWithdraw) {
    Account account = checkAccountExists(accountId);

    validatePositiveOrZero(amountToWithdraw, "Сумма списания не может быть отрицательным");
    validatePositiveOrZero(account.getBalance().subtract(amountToWithdraw), "Не хватает средств на счету");
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToWithdraw), "Сумма списания превышает лимит");
    accountDao.withdraw(accountId, amountToWithdraw);

    return true;
  }

  /**
   * Handles update transaction limit.
   */
  public boolean updateTransactionLimit(Long userId, Long accountId, BigDecimal transactionLimit) {
    validatePositiveOrZero(transactionLimit, "Лимит не может быть отрицательным");
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Изменять лимит можно только для активного счета");
    }

    return accountDao.updateTransactionLimit(accountId, transactionLimit);
  }

  /**
   * Handles deactivate account.
   */
  public boolean deactivateAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Деактивировать можно только активный счет");
    }

    boolean updated = accountDao.setStatusToAccount(accountId, AccountStatus.DEACTIVATED);
    if (updated && Boolean.TRUE.equals(account.getMain())) {
      accountDao.clearMainAccount(userId);
    }

    return updated;
  }

  /**
   * Handles make main account.
   */
  public boolean makeMainAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Основным можно сделать только активный счет");
    }

    accountDao.clearMainAccount(userId);
    return accountDao.setMainAccount(accountId);
  }

  /**
   * Handles approve account.
   */
  public boolean approveAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Одобрить можно только счет в статусе PENDING");
    }

    long activeAccounts = accountDao.countAccountsByUserIdAndStatus(account.getUserId(), AccountStatus.ACTIVE);
    if (activeAccounts >= MAX_ACTIVE_ACCOUNTS) {
      accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
      throw new IllegalArgumentException("У клиента уже 10 активных счетов. Заявка отклонена.");
    }

    boolean approved = accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.ACTIVE);
    if (approved && activeAccounts == 0) {
      accountDao.clearMainAccount(account.getUserId());
      accountDao.setMainAccount(accountId);
    }

    return approved;
  }

  /**
   * Handles reject account.
   */
  public boolean rejectAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Отклонить можно только счет в статусе PENDING");
    }

    return accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  /**
   * Handles top up.
   */
  public boolean topUp(Long accountId, BigDecimal amountToTopUp) {
    Account account = checkAccountExists(accountId);

    validatePositiveOrZero(amountToTopUp, "Сумма списания не может быть отрицательным");
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToTopUp), "Сумма пополнения превышает лимит");
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
      throw new IllegalArgumentException("Счет не найден");
    }
    return account.get();
  }

  private void validateAccountBelongsToUser(Account account, Long userId) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Счет не принадлежит текущему пользователю");
    }
  }

}
