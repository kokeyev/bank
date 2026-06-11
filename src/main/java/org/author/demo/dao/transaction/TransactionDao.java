package org.author.demo.dao.transaction;

import org.author.demo.model.Transaction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionDao {

  boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

  boolean createNewTransaction(Connection connection, Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

  Optional<Transaction> getTransactionById(Long transactionId);

  List<Transaction> getTransactionsByAccountId(Long accountId);

  List<Transaction> getRecentTransactionsByUserId(Long userId, int limit);

}
