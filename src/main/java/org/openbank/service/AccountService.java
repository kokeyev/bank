package org.openbank.service;

import org.openbank.model.Account;
import org.openbank.model.Currency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defines account lifecycle operations.
 */
public interface AccountService {

  /** Creates a pending account request for a client. */
  boolean createNewAccount(Long userId, String currencyName, BigDecimal transactionLimit, String name);

  /** Returns currencies available for account opening. */
  List<Currency> getAllCurrencies();

  /** Returns accounts owned by a user. */
  List<Account> getAccountsByUserId(Long userId);

  /** Returns account requests waiting for approval. */
  List<Account> getPendingAccounts();

  /** Resolves a currency id to its display name. */
  String getCurrencyNameById(Long currencyId);

  /** Withdraws money from an account after validation. */
  boolean withdraw(Long accountId, BigDecimal amountToWithdraw);

  /** Updates the per-operation account limit. */
  boolean updateTransactionLimit(Long userId, Long accountId, BigDecimal transactionLimit);

  /** Deactivates an active account owned by a user. */
  boolean deactivateAccount(Long userId, Long accountId);

  /** Marks one active account as the user's main account. */
  boolean makeMainAccount(Long userId, Long accountId);

  /** Approves a pending account request. */
  boolean approveAccount(Long accountId);

  /** Rejects a pending account request. */
  boolean rejectAccount(Long accountId);

  /** Adds money to an account after validation. */
  boolean topUp(Long accountId, BigDecimal amountToTopUp);
}
