package org.openbank.dao.impl;

import org.openbank.dao.LoanDao;
import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.Loan;
import org.openbank.model.status.LoanStatus;
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
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where status = ?
        order by loan_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, LoanStatus.PENDING.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch loan applications", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Loan> getLoanById(Long loanId) {
    String sql = """
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where loan_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, loanId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch loan", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Loan> getLoansByUserId(Long userId) {
    List<Loan> loans = new ArrayList<>();

    String sql = """
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where user_id = ?
        order by loan_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch user loans", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Loan> getActiveLoansByUserId(Long userId) {
    List<Loan> loans = new ArrayList<>();

    String sql = """
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where user_id = ? and status = ?
        order by loan_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, LoanStatus.ACTIVE.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch active loans", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean createPendingLoan(Long userId, Long loanTypeId, Long accountId, BigDecimal requestedAmount) {
    String sql = """
        insert into loans (user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment)
        values (?, ?, null, ?, ?, null, null, ?, null, null)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setLong(2, loanTypeId);
        statement.setLong(3, accountId);
        statement.setBigDecimal(4, requestedAmount);
        statement.setString(5, LoanStatus.PENDING.name());

        int rowsAffected = statement.executeUpdate();

        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not create loan application", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean createOffer(Long parentLoanId, Long userId, Long loanTypeId, Long accountId, BigDecimal amount, BigDecimal rate, Integer duration, BigDecimal monthlyPayment) {
    String sql = """
        insert into loans (user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment)
        values (?, ?, ?, ?, ?, ?, ?, ?, null, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setLong(2, loanTypeId);
        statement.setLong(3, parentLoanId);
        statement.setLong(4, accountId);
        statement.setBigDecimal(5, amount);
        statement.setBigDecimal(6, rate);
        statement.setInt(7, duration);
        statement.setString(8, LoanStatus.OFFERED.name());
        statement.setBigDecimal(9, monthlyPayment);

        int rowsAffected = statement.executeUpdate();

        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not create loan offer", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Loan> getOffers(Long userId) {
    List<Loan> loans = new ArrayList<>();

    String sql = """
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where user_id = ? and status = ?
        order by loan_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        statement.setString(2, LoanStatus.OFFERED.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            loans.add(map(resultSet));
          }
        }

        return loans;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch loan offers", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean acceptOffer(Long userId, Long loanId) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      Optional<Loan> acceptedOffer = acceptOffer(connection, userId, loanId);
      if (acceptedOffer.isEmpty()) {
        connection.rollback();

        return false;
      }

      connection.commit();

      return true;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }

      throw new BankDataAccessException("Could not accept loan offer", e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Loan> acceptOffer(Connection connection, Long userId, Long loanId) {
    String selectSql = """
        select loan_id, user_id, loan_type_id, parent_loan_id, account_id, remaining_amount, rate, duration, status, start_date, monthly_payment
        from loans
        where loan_id = ? and user_id = ? and status = ?
        for update
        """;

    String activateSql = """
        update loans
        set status = ?, start_date = ?
        where loan_id = ?
        """;

    String rejectSiblingsSql = """
        update loans
        set status = ?
        where parent_loan_id = ? and loan_id <> ? and status = ?
        """;

    String closeParentSql = """
        update loans
        set status = ?
        where loan_id = ?
        """;

    try {
      Loan offer;
      try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
        statement.setLong(1, loanId);
        statement.setLong(2, userId);
        statement.setString(3, LoanStatus.OFFERED.name());
        try (ResultSet resultSet = statement.executeQuery()) {
          if (!resultSet.next()) {
            return Optional.empty();
          }
          offer = map(resultSet);
        }
      }

      try (PreparedStatement statement = connection.prepareStatement(activateSql)) {
        statement.setString(1, LoanStatus.ACTIVE.name());
        statement.setDate(2, Date.valueOf(LocalDate.now()));
        statement.setLong(3, loanId);
        statement.executeUpdate();
      }

      if (offer.getParentLoanId() != null) {
        try (PreparedStatement statement = connection.prepareStatement(rejectSiblingsSql)) {
          statement.setString(1, LoanStatus.REFUSED.name());
          statement.setLong(2, offer.getParentLoanId());
          statement.setLong(3, loanId);
          statement.setString(4, LoanStatus.OFFERED.name());
          statement.executeUpdate();
        }

        try (PreparedStatement statement = connection.prepareStatement(closeParentSql)) {
          statement.setString(1, LoanStatus.CLOSED.name());
          statement.setLong(2, offer.getParentLoanId());
          statement.executeUpdate();
        }
      }

      return Optional.of(offer);
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not accept loan offer", e);
    }
  }
  @Override
  public boolean refuseOffer(Long userId, Long loanId) {
    String sql = """
        update loans
        set status = ?
        where loan_id = ? and user_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, LoanStatus.REFUSED.name());
        statement.setLong(2, loanId);
        statement.setLong(3, userId);
        statement.setString(4, LoanStatus.OFFERED.name());

        int rowsAffected = statement.executeUpdate();

        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not reject loan offer", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean rejectPendingLoan(Long loanId) {
    String sql = """
        update loans
        set status = ?
        where loan_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, LoanStatus.REJECTED.name());
        statement.setLong(2, loanId);
        statement.setString(3, LoanStatus.PENDING.name());

        return statement.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not reject loan application", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
  @Override
  public boolean payLoan(Long loanId, BigDecimal amount) {
    String sql = """
        update loans
        set remaining_amount = greatest(remaining_amount - ?, 0),
            status = case when remaining_amount - ? <= 0 then ? else status end
        where loan_id = ? and status = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, amount);
        statement.setBigDecimal(2, amount);
        statement.setString(3, LoanStatus.CLOSED.name());
        statement.setLong(4, loanId);
        statement.setString(5, LoanStatus.ACTIVE.name());

        int rowsAffected = statement.executeUpdate();

        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not repay loan", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean payLoan(Connection connection, Long loanId, BigDecimal amount) {
    String sql = """
        update loans
        set remaining_amount = greatest(remaining_amount - ?, 0),
            status = case when remaining_amount - ? <= 0 then ? else status end
        where loan_id = ? and status = ?
        """;

    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setBigDecimal(1, amount);
      statement.setBigDecimal(2, amount);
      statement.setString(3, LoanStatus.CLOSED.name());
      statement.setLong(4, loanId);
      statement.setString(5, LoanStatus.ACTIVE.name());

      return statement.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not repay loan", e);
    }
  }

  private Loan map(ResultSet resultSet) throws SQLException {
    Date startDate = resultSet.getDate("start_date");

    return new Loan(
        resultSet.getLong("loan_id"),
        resultSet.getLong("user_id"),
        resultSet.getLong("loan_type_id"),
        getLongOrNull(resultSet, "parent_loan_id"),
        getLongOrNull(resultSet, "account_id"),
        resultSet.getBigDecimal("remaining_amount"),
        resultSet.getBigDecimal("rate"),
        (Integer) resultSet.getObject("duration"),
        resultSet.getString("status"),
        startDate == null ? null : startDate.toLocalDate(),
        resultSet.getBigDecimal("monthly_payment")
    );
  }

  private Long getLongOrNull(ResultSet resultSet, String columnName) throws SQLException {
    long value = resultSet.getLong(columnName);

    return resultSet.wasNull() ? null : value;
  }

  private void rollback(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.rollback();
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not roll back loan changes", e);
    }
  }

  private void resetAutoCommit(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not restore connection settings", e);
    }
  }

}
