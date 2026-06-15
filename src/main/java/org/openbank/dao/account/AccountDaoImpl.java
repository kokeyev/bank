package org.openbank.dao.account;

import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.Account;
import org.openbank.model.status.AccountStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountDaoImpl implements AccountDao {

  private final ConnectionPool connectionPool;

  public AccountDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public boolean createNewAccount(Long userId, String cardNumber, String cvv, LocalDate expiryDate, BigDecimal balance, Long currencyId, AccountStatus status, BigDecimal transactionLimit, String name, Boolean main) {
    String sql = """
        insert into accounts (user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, cardNumber);
        statement.setString(3, cvv);
        statement.setDate(4, Date.valueOf(expiryDate));
        statement.setBigDecimal(5, balance);
        statement.setLong(6, currencyId);
        statement.setString(7, status.name());
        statement.setBigDecimal(8, transactionLimit);
        statement.setString(9, name);
        statement.setBoolean(10, Boolean.TRUE.equals(main));

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось создать счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Account> getAccountById(Long accountId) {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, accountId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Account> getAccountByIdForUpdate(Connection connection, Long accountId) {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where account_id = ?
        for update
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, accountId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(map(resultSet));
        }
      }

      return Optional.empty();
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить счет", e);
    }
  }

  @Override
  public Optional<Account> getAccountByCardNumber(String cardNumber) {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where card_number = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, cardNumber);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить счет по номеру карты", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Account> getMainActiveAccountByUserId(Long userId) {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where user_id = ? and status = ? and is_main = true
        limit 1
        """;

    return getSingleUserAccount(userId, sql);
  }

  @Override
  public Optional<Account> getFirstActiveAccountByUserId(Long userId) {
    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where user_id = ? and status = ?
        order by is_main desc, account_id
        limit 1
        """;

    return getSingleUserAccount(userId, sql);
  }

  private Optional<Account> getSingleUserAccount(Long userId, String sql) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, AccountStatus.ACTIVE.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить активный счет пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Account> getAccountsByUserId(Long userId) {
    List<Account> accounts = new ArrayList<>();

    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where user_id = ?
        order by is_main desc, account_id
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            accounts.add(map(resultSet));
          }
        }

        return accounts;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить счета пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Account> getAccountsByStatus(AccountStatus status) {
    List<Account> accounts = new ArrayList<>();

    String sql = """
        select account_id, user_id, card_number, cvv, expiry_date, balance, currency_id, status, transaction_limit, name, is_main
        from accounts
        where status = ?
        order by account_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, status.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            accounts.add(map(resultSet));
          }
        }

        return accounts;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить счета по статусу", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public long countAccountsByUserIdAndStatus(Long userId, AccountStatus status) {
    String sql = """
        select count(*)
        from accounts
        where user_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, status.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          resultSet.next();
          return resultSet.getLong(1);
        }
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось посчитать счета пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean setStatusToAccount(Long accountId, AccountStatus status) {
    String sql = """
        update accounts
        set status = ?
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, status.name());
        statement.setLong(2, accountId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить статус счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean setStatusToAccount(Long accountId, AccountStatus currentStatus, AccountStatus newStatus) {
    String sql = """
        update accounts
        set status = ?
        where account_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newStatus.name());
        statement.setLong(2, accountId);
        statement.setString(3, currentStatus.name());

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить статус счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean updateTransactionLimit(Long accountId, BigDecimal transactionLimit) {
    String sql = """
        update accounts
        set transaction_limit = ?
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, transactionLimit);
        statement.setLong(2, accountId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить лимит счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean clearMainAccount(Long userId) {
    String sql = """
        update accounts
        set is_main = false
        where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.executeUpdate();
        return true;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось сбросить основной счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean setMainAccount(Long accountId) {
    String sql = """
        update accounts
        set is_main = true
        where account_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, accountId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось сделать счет основным", e);
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
      throw new BankDataAccessException("Не удалось снять деньги со счета", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean withdraw(Connection connection, Long accountId, BigDecimal amountToWithdraw) {
    String sql = """
        update accounts
        set balance = balance - ?
        where account_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amountToWithdraw);
      statement.setLong(2, accountId);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось снять деньги со счета", e);
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
      throw new BankDataAccessException("Не удалось пополнить счет", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean topUp(Connection connection, Long accountId, BigDecimal amountToTopUp) {
    String sql = """
        update accounts
        set balance = balance + ?
        where account_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amountToTopUp);
      statement.setLong(2, accountId);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось пополнить счет", e);
    }
  }


  private Account map(ResultSet resultSet) throws SQLException {
    Date expiryDate = resultSet.getDate("expiry_date");

    return new Account(
        resultSet.getLong("account_id"),
        resultSet.getLong("user_id"),
        resultSet.getString("card_number"),
        resultSet.getString("cvv"),
        expiryDate == null ? null : expiryDate.toLocalDate(),
        resultSet.getBigDecimal("balance"),
        resultSet.getLong("currency_id"),
        resultSet.getString("status"),
        resultSet.getBigDecimal("transaction_limit"),
        resultSet.getString("name"),
        resultSet.getBoolean("is_main")
    );
  }
}
