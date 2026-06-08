package org.author.demo.dao.transaction;

import org.author.demo.db.ConnectionPool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class TransactionDaoImpl implements TransactionDao {

  private final ConnectionPool connectionPool;

  public TransactionDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    String sql = """
        insert into transactions (sender_account_id, receiver_account_id, transaction_date, amount, currency_id, fee, message, transaction_type)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, senderAccountId);
        statement.setLong(2, receiverAccountId);
        statement.setDate(3, Date.valueOf(transactionDate.toLocalDate()));
        statement.setBigDecimal(4, amount);
        statement.setLong(5, currencyId);
        statement.setBigDecimal(6, fee);
        statement.setString(7, message);
        statement.setString(8, transactionType);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось выполнить транзакцию", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
}
