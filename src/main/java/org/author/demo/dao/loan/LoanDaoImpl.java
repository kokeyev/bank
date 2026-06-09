package org.author.demo.dao.loan;

import org.author.demo.db.ConnectionPool;
import org.author.demo.model.Loan;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LoanDaoImpl implements LoanDao {

  private final ConnectionPool connectionPool;

  public LoanDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public List<Loan> getPendingLoans() {

    List<Loan> loans = new ArrayList<>();

    String sql = """
        select loan_id, user_id, loan_type_id, remaining_amount, status, start_date, monthly_payment
        from loans
        where status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, "Pending");
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось получить заявки на кредит", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean createOffer(Long userId, Long loanTypeId, BigDecimal remainingAmount, String status, LocalDate startDate, BigDecimal monthlyPayment) {
    String sql = """
        insert into loans (user_id, loan_type_id, remaining_amount, status, start_date, monthly_payment)
        values (?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setLong(2, loanTypeId);
        statement.setBigDecimal(3, remainingAmount);
        statement.setString(4, status);
        statement.setDate(5, Date.valueOf(startDate));
        statement.setBigDecimal(6, monthlyPayment);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось создать предложение по кредиту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<List<Loan>> getOffers(Long userId) {
    List<Loan> loans = new ArrayList<>();

    String sql = """
        select loan_id, user_id, loan_type_id, remaining_amount, status, start_date, monthly_payment
        from loans
        where user_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, "Offer");
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans.isEmpty() ? Optional.empty() : Optional.of(loans);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось получить предложения по кредиту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean acceptOffer(Long loanId) {
    String sql = """
        update loans
        set status = ?
        where loan_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, "Accepted");
        statement.setLong(2, loanId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось принять предложение по кредиту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public void refuseOffer(Long loanId) {
    String sql = """
        update loans
        set status = ?
        where loan_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, "Refused");
        statement.setLong(2, loanId);

        statement.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось отклонить предложение по кредиту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  private Loan map(ResultSet resultSet) throws SQLException {
    Date startDate = resultSet.getDate("start_date");

    return new Loan(
        resultSet.getLong("loan_id"),
        resultSet.getLong("user_id"),
        resultSet.getLong("loan_type_id"),
        resultSet.getBigDecimal("remaining_amount"),
        resultSet.getString("status"),
        startDate == null ? null : startDate.toLocalDate(),
        resultSet.getBigDecimal("monthly_payment")
    );
  }

}
