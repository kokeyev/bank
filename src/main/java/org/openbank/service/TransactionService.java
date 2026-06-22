package org.openbank.service;

import org.openbank.model.Transaction;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defines money movement and transaction history operations.
 */
public interface TransactionService {

  /** Records transaction history without balance changes. */
  boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

  /** Returns transactions connected to one account. */
  List<Transaction> getTransactionsByAccountId(Long accountId);

  /** Returns a user's newest transactions. */
  List<Transaction> getRecentTransactionsByUserId(Long userId, int limit);

  /** Returns one page of a user's transactions. */
  List<Transaction> getTransactionsByUserId(Long userId, int limit, int offset);

  /** Counts a user's transactions for pagination. */
  int countTransactionsByUserId(Long userId);

  /** Transfers money between accounts of one client. */
  boolean makeTransactionBetweenAccountsOfOneClient(Long senderAccountId, Long receiverAccountId, BigDecimal amount);

  /** Transfers money between accounts owned by the current user. */
  boolean makeTransactionBetweenAccountsOfOneClient(Long userId, Long senderAccountId, Long receiverAccountId, BigDecimal amount, String message);

  /** Transfers money to another user by phone number. */
  boolean makeTransactionByPhoneNumber(Long senderAccountId, String receiverPhoneNumber, BigDecimal amount);

  /** Transfers money to an internal or external card. */
  boolean makeTransactionByCardNumber(Long senderAccountId, String receiverCardNumber, BigDecimal amount);

  /** Pays an active loan from a client account. */
  boolean makeTransactionTopUpLoan(Long senderAccountId, Long loanId, BigDecimal amount);

  /** Exchanges money between two accounts. */
  boolean makeTransactionExchangeCurrencies(Long senderAccountId, Long receiverAccountId, BigDecimal amount);
}
