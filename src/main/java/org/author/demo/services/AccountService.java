package org.author.demo.services;

import org.author.demo.dao.account.AccountDao;
import org.author.demo.dao.currency.CurrencyDao;
import org.author.demo.model.Account;
import org.author.demo.model.Currency;
import org.author.demo.model.status.AccountStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

  public boolean createNewAccount(Long userId, String currencyName, BigDecimal transactionLimit, String name) {
    validateText(name, "Введите название счета");
    validateText(currencyName, "Выберите валюту");
    validatePositiveOrZero(transactionLimit, "Лимит не может быть отрицательным");

    Currency currency = currencyDao.getCurrencyByName(currencyName).orElseThrow(() -> new RuntimeException("Валюта не найдена"));

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

  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  public List<Account> getAccountsByUserId(Long userId) {
    return accountDao.getAccountsByUserId(userId);
  }

  public List<Account> getPendingAccounts() {
    return accountDao.getAccountsByStatus(AccountStatus.PENDING);
  }

  public String getCurrencyNameById(Long currencyId) {
    return currencyDao.getCurrencyNameById(currencyId);
  }

  public boolean withdraw(Long accountId, BigDecimal amountToWithdraw) {
    Account account = checkAccountExists(accountId);

    // check amountToWithdraw > 0
    validatePositiveOrZero(amountToWithdraw, "Сумма списания не может быть отрицательным");

    // check account has balance > amountToWithdraw
    validatePositiveOrZero(account.getBalance().subtract(amountToWithdraw), "Не хватает средств на счету");

    // check transaction limit >= amountToWithdraw
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToWithdraw), "Сумма списания превышает лимит");

    // everything is fine, we can withdraw
    accountDao.withdraw(accountId, amountToWithdraw);

    return true;
  }

  public boolean updateTransactionLimit(Long userId, Long accountId, BigDecimal transactionLimit) {
    validatePositiveOrZero(transactionLimit, "Лимит не может быть отрицательным");
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Изменять лимит можно только для активного счета");
    }

    return accountDao.updateTransactionLimit(accountId, transactionLimit);
  }

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

  public boolean makeMainAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Основным можно сделать только активный счет");
    }

    accountDao.clearMainAccount(userId);
    return accountDao.setMainAccount(accountId);
  }

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

  public boolean rejectAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Отклонить можно только счет в статусе PENDING");
    }

    return accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  public boolean topUp(Long accountId, BigDecimal amountToTopUp) {
    Account account = checkAccountExists(accountId);

    // check amountToTopUp > 0
    validatePositiveOrZero(amountToTopUp, "Сумма списания не может быть отрицательным");

    // check transaction limit >= amountToTopUp
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToTopUp), "Сумма пополнения превышает лимит");

    // everything is fine, we can topUp
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
