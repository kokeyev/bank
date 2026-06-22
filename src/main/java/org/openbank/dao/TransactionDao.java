package org.openbank.dao;

import org.openbank.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Defines the transaction dao contract.
 */
public interface TransactionDao {

  /** Creates a transaction record. */
  boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

  /** Creates a transaction record using an existing transaction. */
  boolean createNewTransaction(Connection connection, Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

  /** Finds a transaction by id. */
  Optional<Transaction> getTransactionById(Long transactionId);

  /** Returns transactions for an account. */
  List<Transaction> getTransactionsByAccountId(Long accountId);

  /** Returns recent transactions for a user. */
  List<Transaction> getRecentTransactionsByUserId(Long userId, int limit);

  /** Returns paged transactions for a user. */
  List<Transaction> getTransactionsByUserId(Long userId, int limit, int offset);

  /** Counts transactions for a user. */
  int countTransactionsByUserId(Long userId);

}
