package org.author.demo.dao.loanType;

import org.author.demo.db.ConnectionPool;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Repository
public class LoanTypeDaoImpl implements LoanTypeDao {

  private final ConnectionPool connectionPool;

  public LoanTypeDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public boolean createNewTypeOfLoan(String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId) {
    String sql = """
        insert into loan_types (name, rate, duration, minimum_amount, maximum_amount, currency_id)
        values (?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, name);
        statement.setBigDecimal(2, rate);
        statement.setInt(3, duration);
        statement.setBigDecimal(4, minimumAmount);
        statement.setBigDecimal(5, maximumAmount);
        statement.setLong(6, currencyId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось создать тип кредита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
}
