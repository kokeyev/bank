package org.author.demo.dao.account;

import org.author.demo.db.ConnectionPool;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

@Repository
public class AccountDaoImpl implements AccountDao {

  private final ConnectionPool connectionPool;

  public AccountDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public boolean createNewAccount(Long userId, String cardNumber, LocalDate expiryDate, BigDecimal balance, Long currencyId, String status, BigDecimal transactionLimit, String name) {
    String sql = """
        insert into accounts (user_id, card_number, expiry_date, balance, currency_id, status, transaction_limit, name)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, cardNumber);
        statement.setDate(3, Date.valueOf(expiryDate));
        statement.setBigDecimal(4, balance);
        statement.setLong(5, currencyId);
        statement.setString(6, status);
        statement.setBigDecimal(7, transactionLimit);
        statement.setString(8, name);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось создать счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean setStatusToAccount(Long accountId, String status) {
    String sql = """
        update accounts
        set status = ?
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, status);
        statement.setLong(2, accountId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось изменить статус счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean withdraw(Long accountId, BigDecimal amountToWithdraw) {
    String sql = """
        update accounts
        set balance = balance - ?
        where account_id = ? and balance >= ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, amountToWithdraw);
        statement.setLong(2, accountId);
        statement.setBigDecimal(3, amountToWithdraw);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось снять деньги со счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean topUp(Long accountId, BigDecimal amountToTopUp) {
    String sql = """
        update accounts
        set balance = balance + ?
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, amountToTopUp);
        statement.setLong(2, accountId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось пополнить счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
}
