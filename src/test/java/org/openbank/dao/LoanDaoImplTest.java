package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.LoanDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Loan;
import org.openbank.model.status.LoanStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanDaoImplTest {

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private PreparedStatement activateStatement;

  @Mock
  private PreparedStatement rejectSiblingsStatement;

  @Mock
  private PreparedStatement closeParentStatement;

  @Mock
  private ResultSet resultSet;

  private LoanDaoImpl dao;

  @BeforeEach
  void setUp() throws SQLException {
    dao = new LoanDaoImpl(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
  }

  @Test
  void getPendingLoansMapsRows() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, false);
    loanRow(15L, 7L, 2L, null, LoanStatus.PENDING);

    List<Loan> loans = dao.getPendingLoans();

    assertEquals(1, loans.size());
    assertEquals(15L, loans.getFirst().getLoanId());
    assertEquals(LoanStatus.PENDING.name(), loans.getFirst().getStatus());
    verify(statement).setString(1, LoanStatus.PENDING.name());
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void createOfferBindsManagerCalculatedFields() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(1);

    boolean created = dao.createOffer(
        10L,
        7L,
        2L,
        new BigDecimal("1000000"),
        new BigDecimal("21.5"),
        24,
        new BigDecimal("52000")
    );

    assertTrue(created);
    verify(statement).setLong(1, 7L);
    verify(statement).setLong(2, 2L);
    verify(statement).setLong(3, 10L);
    verify(statement).setBigDecimal(4, new BigDecimal("1000000"));
    verify(statement).setBigDecimal(5, new BigDecimal("21.5"));
    verify(statement).setInt(6, 24);
    verify(statement).setString(7, LoanStatus.OFFERED.name());
    verify(statement).setBigDecimal(8, new BigDecimal("52000"));
  }

  @Test
  void acceptOfferActivatesSelectedOfferAndClosesParentRequest() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement, activateStatement, rejectSiblingsStatement, closeParentStatement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    loanRow(15L, 7L, 2L, 10L, LoanStatus.OFFERED);

    assertTrue(dao.acceptOffer(7L, 15L));

    verify(connection).setAutoCommit(false);
    verify(activateStatement).setString(1, LoanStatus.ACTIVE.name());
    verify(activateStatement).setLong(3, 15L);
    verify(rejectSiblingsStatement).setString(1, LoanStatus.REFUSED.name());
    verify(rejectSiblingsStatement).setLong(2, 10L);
    verify(closeParentStatement).setString(1, LoanStatus.CLOSED.name());
    verify(closeParentStatement).setLong(2, 10L);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  void acceptOfferWithoutParentOnlyActivatesOffer() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement, activateStatement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    loanRow(15L, 7L, 2L, null, LoanStatus.OFFERED);

    assertTrue(dao.acceptOffer(7L, 15L));

    verify(activateStatement).setString(1, LoanStatus.ACTIVE.name());
    verify(activateStatement).setLong(3, 15L);
    verify(connection).commit();
  }

  @Test
  void acceptOfferReturnsFalseWhenOfferNotFound() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertEquals(false, dao.acceptOffer(7L, 15L));

    verify(connection).rollback();
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void getLoanByIdReturnsEmptyWhenNoRowExists() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertTrue(dao.getLoanById(15L).isEmpty());
  }

  @Test
  void payLoanWrapsSqlException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException("update failed"));

    assertThrows(BankDataAccessException.class, () -> dao.payLoan(15L, BigDecimal.TEN));
  }

  private void loanRow(Long loanId, Long userId, Long loanTypeId, Long parentLoanId, LoanStatus status) throws SQLException {
    when(resultSet.getLong("loan_id")).thenReturn(loanId);
    when(resultSet.getLong("user_id")).thenReturn(userId);
    when(resultSet.getLong("loan_type_id")).thenReturn(loanTypeId);
    when(resultSet.getLong("parent_loan_id")).thenReturn(parentLoanId == null ? 0L : parentLoanId);
    when(resultSet.wasNull()).thenReturn(parentLoanId == null);
    when(resultSet.getBigDecimal("remaining_amount")).thenReturn(new BigDecimal("1000000"));
    when(resultSet.getBigDecimal("rate")).thenReturn(new BigDecimal("21.5"));
    when(resultSet.getObject("duration")).thenReturn(24);
    when(resultSet.getString("status")).thenReturn(status.name());
    when(resultSet.getDate("start_date")).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 1)));
    when(resultSet.getBigDecimal("monthly_payment")).thenReturn(new BigDecimal("52000"));
  }
}
