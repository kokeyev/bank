package org.openbank.dao;

import org.openbank.model.Account;
import org.openbank.model.status.AccountStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Defines the account dao contract.
 */
public interface AccountDao {

  /** Creates a new account. */
  boolean createNewAccount(Long userId, String cardNumber, String cvv, LocalDate expiryDate, BigDecimal balance, Long currencyId, AccountStatus status, BigDecimal transactionLimit, String name, Boolean main);

  /** Finds an account by id. */
  Optional<Account> getAccountById(Long accountId);

  /** Finds and locks an account by id in an existing transaction. */
  Optional<Account> getAccountByIdForUpdate(Connection connection, Long accountId);

  /** Finds an account by card number. */
  Optional<Account> getAccountByCardNumber(String cardNumber);

  /** Finds the main active account for a user. */
  Optional<Account> getMainActiveAccountByUserId(Long userId);

  /** Finds the first active account for a user. */
  Optional<Account> getFirstActiveAccountByUserId(Long userId);

  /** Finds and locks the first active account for a user and currency. */
  Optional<Account> getFirstActiveAccountByUserIdAndCurrencyIdForUpdate(Connection connection, Long userId, Long currencyId);

  /** Returns all accounts for a user. */
  List<Account> getAccountsByUserId(Long userId);

  /** Returns accounts with the given status. */
  List<Account> getAccountsByStatus(AccountStatus status);

  /** Counts user accounts with the given status. */
  long countAccountsByUserIdAndStatus(Long userId, AccountStatus status);

  /** Updates an account status. */
  boolean setStatusToAccount(Long accountId, AccountStatus status);

  /** Updates an account status when the current status matches. */
  boolean setStatusToAccount(Long accountId, AccountStatus currentStatus, AccountStatus newStatus);

  /** Updates an account transaction limit. */
  boolean updateTransactionLimit(Long accountId, BigDecimal transactionLimit);

  /** Clears the main account flag for a user. */
  boolean clearMainAccount(Long userId);

  /** Marks an account as main. */
  boolean setMainAccount(Long accountId);

  /** Withdraws money from an account. */
  boolean withdraw(Long accountId, BigDecimal amountToWithdraw);

  /** Withdraws money using an existing transaction. */
  boolean withdraw(Connection connection, Long accountId, BigDecimal amountToWithdraw);

  /** Tops up an account. */
  boolean topUp(Long accountId, BigDecimal amountToTopUp);

  /** Tops up an account using an existing transaction. */
  boolean topUp(Connection connection, Long accountId, BigDecimal amountToTopUp);

}
