package org.author.demo.dao.deposit;

import org.author.demo.db.ConnectionPool;
import org.author.demo.model.Deposit;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class DepositDaoImpl implements DepositDao {

  private final ConnectionPool connectionPool;

  public DepositDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public List<Deposit> getPendingDeposits() {
    List<Deposit> deposits = new ArrayList<>();

    String sql = """
        select deposit_id, user_id, deposit_type_id, reinvest_interest, status, start_date, current_amount
        from deposits
        where status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, "Pending");
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            deposits.add(map(resultSet));
          }
        }

        return deposits;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось получить заявки на депозит", e);
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
        statement.setString(1, "Accepted");
        statement.setLong(2, depositId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось принять депозит", e);
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
        resultSet.getString("status"),
        startDate == null ? null : startDate.toLocalDate(),
        resultSet.getBigDecimal("current_amount")
    );
  }
}
