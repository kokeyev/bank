package org.author.demo.dao.account;

import org.author.demo.model.Account;
import org.author.demo.model.status.AccountStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountDao {

  boolean createNewAccount(Long userId, String cardNumber, String cvv, LocalDate expiryDate, BigDecimal balance, Long currencyId, AccountStatus status, BigDecimal transactionLimit, String name, Boolean main);

  Optional<Account> getAccountById(Long accountId);

  Optional<Account> getAccountByCardNumber(String cardNumber);

  Optional<Account> getMainActiveAccountByUserId(Long userId);

  Optional<Account> getFirstActiveAccountByUserId(Long userId);

  List<Account> getAccountsByUserId(Long userId);

  List<Account> getAccountsByStatus(AccountStatus status);

  long countAccountsByUserIdAndStatus(Long userId, AccountStatus status);

  boolean setStatusToAccount(Long accountId, AccountStatus status);

  boolean setStatusToAccount(Long accountId, AccountStatus currentStatus, AccountStatus newStatus);

  boolean updateTransactionLimit(Long accountId, BigDecimal transactionLimit);

  boolean clearMainAccount(Long userId);

  boolean setMainAccount(Long accountId);

  boolean withdraw(Long accountId, BigDecimal amountToWithdraw);

  boolean topUp(Long accountId, BigDecimal amountToTopUp);

}
