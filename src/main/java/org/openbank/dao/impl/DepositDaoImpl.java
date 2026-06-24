package org.openbank.dao.impl;

import org.openbank.dao.DepositDao;
import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.Deposit;
import org.openbank.model.status.DepositStatus;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
@Repository
public class DepositDaoImpl implements DepositDao {

  private final ConnectionPool connectionPool;
  public DepositDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }
  @Override
  public boolean createDeposit(Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      return createDeposit(connection, userId, depositTypeId, reinvestInterest, autoRenewal, status, startDate, currentAmount);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean createDeposit(Connection connection, Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount) {
    String sql = """
        insert into deposits (user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount)
        values (?, ?, ?, ?, ?, ?, ?)
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, userId);
      statement.setLong(2, depositTypeId);
      statement.setBoolean(3, reinvestInterest);
      statement.setBoolean(4, autoRenewal);
      statement.setString(5, status.name());
      statement.setDate(6, Date.valueOf(startDate));
      statement.setBigDecimal(7, currentAmount);

      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not create deposit", e);
    }
  }
  @Override
  public Optional<Deposit> getDepositById(Long depositId) {
    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount
        from deposits
        where deposit_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, depositId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch deposit", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public Optional<Deposit> getDepositByIdForUpdate(Connection connection, Long depositId) {
    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount
        from deposits
        where deposit_id = ?
        for update
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, depositId);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (resultSet.next()) {
          return Optional.of(map(resultSet));
        }
      }

      return Optional.empty();
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch deposit", e);
    }
  }
  @Override
  public List<Deposit> getDepositsByUserId(Long userId) {
    List<Deposit> deposits = new ArrayList<>();

    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount
        from deposits
        where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            deposits.add(map(resultSet));
          }
        }

        return deposits;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch user deposits", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public List<Deposit> getDepositsByStatus(DepositStatus status) {
    List<Deposit> deposits = new ArrayList<>();

    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount
        from deposits
        where status = ?
        order by deposit_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, status.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            deposits.add(map(resultSet));
          }
        }

        return deposits;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch deposits by status", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean topUpDeposit(Connection connection, Long depositId, BigDecimal amount) {
    String sql = """
        update deposits
        set current_amount = current_amount + ?
        where deposit_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, depositId);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not top up deposit", e);
    }
  }
  @Override
  public boolean withdrawFromDeposit(Connection connection, Long depositId, BigDecimal amount) {
    String sql = """
        update deposits
        set current_amount = current_amount - ?
        where deposit_id = ? and current_amount >= ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setLong(2, depositId);
      statement.setBigDecimal(3, amount);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not withdraw from deposit", e);
    }
  }
  @Override
  public boolean setStatus(Long depositId, DepositStatus status) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      return setStatus(connection, depositId, status);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean setStatus(Connection connection, Long depositId, DepositStatus status) {
    String sql = """
        update deposits
        set status = ?
        where deposit_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, status.name());
      statement.setLong(2, depositId);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not update deposit status", e);
    }
  }
  @Override
  public boolean updateStartDate(Connection connection, Long depositId, LocalDate startDate) {
    String sql = """
        update deposits
        set start_date = ?
        where deposit_id = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setDate(1, Date.valueOf(startDate));
      statement.setLong(2, depositId);
      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not update deposit date", e);
    }
  }
  @Override
  public List<Deposit> getPendingDeposits() {
    List<Deposit> deposits = new ArrayList<>();

    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, auto_renewal, status, start_date, current_amount
        from deposits
        where status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, DepositStatus.PENDING.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            deposits.add(map(resultSet));
          }
        }

        return deposits;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch deposit applications", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean acceptDeposit(Long depositId) {
    String sql = """
        update deposits
        set status = ?
        where deposit_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, DepositStatus.ACTIVE.name());
        statement.setLong(2, depositId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not accept deposit", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  private Deposit map(ResultSet resultSet) throws SQLException {
    Date startDate = resultSet.getDate("start_date");

    return new Deposit(
        resultSet.getLong("deposit_id"),
        resultSet.getLong("user_id"),
        resultSet.getLong("deposit_type_id"),
        resultSet.getBoolean("reinvest_interest"),
        resultSet.getBoolean("auto_renewal"),
        resultSet.getString("status"),
        startDate == null ? null : startDate.toLocalDate(),
        resultSet.getBigDecimal("current_amount")
    );
  }
}
