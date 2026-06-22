package org.openbank.dao.impl;

import org.openbank.dao.LoanTypeDao;
import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.LoanType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
      throw new BankDataAccessException("Не удалось создать тип кредита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public Optional<LoanType> getLoanTypeById(Long loanTypeId) {
    String sql = """
        select loan_type_id, name, rate, duration, minimum_amount, maximum_amount, currency_id
        from loan_types
        where loan_type_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, loanTypeId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить тип кредита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public List<LoanType> getAllLoanTypes() {
    List<LoanType> loanTypes = new ArrayList<>();

    String sql = """
        select loan_type_id, name, rate, duration, minimum_amount, maximum_amount, currency_id
        from loan_types
        order by name
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loanTypes.add(map(resultSet));
          }
        }

        return loanTypes;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить типы кредитов", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean changeRateOfLoanType(Long loanTypeId, BigDecimal newRate) {
    String sql = """
        update loan_types
        set rate = ?
        where loan_type_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, newRate);
        statement.setLong(2, loanTypeId);
        return statement.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить ставку типа кредита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  private LoanType map(ResultSet resultSet) throws SQLException {
    return new LoanType(
        resultSet.getLong("loan_type_id"),
        resultSet.getString("name"),
        resultSet.getBigDecimal("rate"),
        resultSet.getInt("duration"),
        resultSet.getBigDecimal("minimum_amount"),
        resultSet.getBigDecimal("maximum_amount"),
        resultSet.getLong("currency_id")
    );
  }
}
