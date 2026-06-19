package org.openbank.dao.transaction;

import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.Transaction;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Repository
public class TransactionDaoImpl implements TransactionDao {

  private final ConnectionPool connectionPool;
  public TransactionDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }
  @Override
  public boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      return createNewTransaction(connection, senderAccountId, receiverAccountId, transactionDate, amount, currencyId, fee, message, transactionType);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean createNewTransaction(Connection connection, Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    String sql = """
        insert into transactions (sender_account_id, receiver_account_id, transaction_date, amount, currency_id, fee, message, transaction_type)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setObject(1, senderAccountId);
      statement.setObject(2, receiverAccountId);
      statement.setTimestamp(3, Timestamp.valueOf(transactionDate));
      statement.setBigDecimal(4, amount);
      statement.setLong(5, currencyId);
      statement.setBigDecimal(6, fee);
      statement.setString(7, message);
      statement.setString(8, transactionType);

      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось сохранить историю транзакции", e);
    }
  }
  @Override
  public Optional<Transaction> getTransactionById(Long transactionId) {
    String sql = """
        select transaction_id, sender_account_id, receiver_account_id, transaction_date, amount, currency_id, fee, message, transaction_type
        from transactions
        where transaction_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, transactionId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить транзакцию", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public List<Transaction> getTransactionsByAccountId(Long accountId) {
    List<Transaction> transactions = new ArrayList<>();

    String sql = """
        select transaction_id, sender_account_id, receiver_account_id, transaction_date, amount, currency_id, fee, message, transaction_type
        from transactions
        where sender_account_id = ? or receiver_account_id = ?
        order by transaction_date desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, accountId);
        statement.setLong(2, accountId);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            transactions.add(map(resultSet));
          }
        }

        return transactions;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить транзакции счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public List<Transaction> getRecentTransactionsByUserId(Long userId, int limit) {
    return getTransactionsByUserId(userId, limit, 0);
  }
  @Override
  public List<Transaction> getTransactionsByUserId(Long userId, int limit, int offset) {
    List<Transaction> transactions = new ArrayList<>();

    String sql = """
        select distinct t.transaction_id, t.sender_account_id, t.receiver_account_id, t.transaction_date, t.amount, t.currency_id, t.fee, t.message, t.transaction_type
        from transactions t
        left join accounts sender on sender.account_id = t.sender_account_id
        left join accounts receiver on receiver.account_id = t.receiver_account_id
        where sender.user_id = ? or receiver.user_id = ?
        order by t.transaction_date desc
        limit ?
        offset ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setLong(2, userId);
        statement.setInt(3, limit);
        statement.setInt(4, offset);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            transactions.add(map(resultSet));
          }
        }

        return transactions;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить историю транзакций", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public int countTransactionsByUserId(Long userId) {
    String sql = """
        select count(distinct t.transaction_id)
        from transactions t
        left join accounts sender on sender.account_id = t.sender_account_id
        left join accounts receiver on receiver.account_id = t.receiver_account_id
        where sender.user_id = ? or receiver.user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setLong(2, userId);
        try (ResultSet resultSet = statement.executeQuery()) {
          return resultSet.next() ? resultSet.getInt(1) : 0;
        }
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось посчитать историю транзакций", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }


  private Transaction map(ResultSet resultSet) throws SQLException {
    Timestamp transactionDate = resultSet.getTimestamp("transaction_date");

    return new Transaction(
        resultSet.getLong("transaction_id"),
        getLongOrNull(resultSet, "sender_account_id"),
        getLongOrNull(resultSet, "receiver_account_id"),
        transactionDate == null ? null : transactionDate.toLocalDateTime(),
        resultSet.getBigDecimal("amount"),
        resultSet.getLong("currency_id"),
        resultSet.getBigDecimal("fee"),
        resultSet.getString("message"),
        resultSet.getString("transaction_type")
    );
  }

  private Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
    long value = resultSet.getLong(columnName);
    return resultSet.wasNull() ? null : value;
  }
}
