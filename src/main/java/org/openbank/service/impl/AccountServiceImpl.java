package org.openbank.service.impl;

import org.openbank.service.AccountService;
import org.openbank.service.BankCardGenerator;
import org.openbank.service.MessageService;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.status.AccountStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AccountServiceImpl implements AccountService {

  private static final int MAX_ACTIVE_ACCOUNTS = 10;

  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final BankCardGenerator bankCardGenerator;
  private final MessageService messageService;

  public AccountServiceImpl(AccountDao accountDao, CurrencyDao currencyDao, BankCardGenerator bankCardGenerator, MessageService messageService) {
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.bankCardGenerator = bankCardGenerator;
    this.messageService = messageService;
  }

  public boolean createNewAccount(Long userId, String currencyName, BigDecimal transactionLimit, String name) {
    validateText(name, messageService.get("validation.accountName.required"));
    validateText(currencyName, messageService.get("validation.currency.required"));
    validatePositiveOrZero(transactionLimit, messageService.get("account.validation.limit.negative"));

    Currency currency = currencyDao.getCurrencyByName(currencyName).orElseThrow(() -> new BankDataAccessException(messageService.get("error.currency.notFound")));

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

    validatePositiveOrZero(amountToWithdraw, messageService.get("account.validation.withdraw.negative"));
    validatePositiveOrZero(account.getBalance().subtract(amountToWithdraw), messageService.get("account.validation.insufficientFunds"));
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToWithdraw), messageService.get("account.validation.withdraw.limitExceeded"));
    accountDao.withdraw(accountId, amountToWithdraw);

    return true;
  }

  public boolean updateTransactionLimit(Long userId, Long accountId, BigDecimal transactionLimit) {
    validatePositiveOrZero(transactionLimit, messageService.get("account.validation.limit.negative"));
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(messageService.get("account.validation.limit.activeOnly"));
    }

    return accountDao.updateTransactionLimit(accountId, transactionLimit);
  }

  public boolean deactivateAccount(Long userId, Long accountId) {
    Account account = checkAccountExists(accountId);
    validateAccountBelongsToUser(account, userId);

    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(messageService.get("account.validation.deactivate.activeOnly"));
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
      throw new IllegalArgumentException(messageService.get("account.validation.main.activeOnly"));
    }

    accountDao.clearMainAccount(userId);

    return accountDao.setMainAccount(accountId);
  }

  public boolean approveAccount(Long accountId) {
    Account account = checkAccountExists(accountId);

    if (!AccountStatus.PENDING.name().equals(account.getStatus())) {
      throw new IllegalArgumentException(messageService.get("account.validation.approve.pendingOnly"));
    }

    long activeAccounts = accountDao.countAccountsByUserIdAndStatus(account.getUserId(), AccountStatus.ACTIVE);
    if (activeAccounts >= MAX_ACTIVE_ACCOUNTS) {
      accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);

      throw new IllegalArgumentException(messageService.get("account.validation.maxActiveAccounts"));
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
      throw new IllegalArgumentException(messageService.get("account.validation.reject.pendingOnly"));
    }

    return accountDao.setStatusToAccount(accountId, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  public boolean topUp(Long accountId, BigDecimal amountToTopUp) {
    Account account = checkAccountExists(accountId);

    validatePositiveOrZero(amountToTopUp, messageService.get("account.validation.withdraw.negative"));
    validatePositiveOrZero(account.getTransactionLimit().subtract(amountToTopUp), messageService.get("account.validation.topUp.limitExceeded"));
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
      throw new IllegalArgumentException(messageService.get("error.account.notFound"));
    }

    return account.get();
  }

  private void validateAccountBelongsToUser(Account account, Long userId) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException(messageService.get("account.validation.notOwner"));
    }
  }

}
