package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

  private static final Long LOAN_ID = 15L;
  private static final Long USER_ID = 7L;
  private static final Long LOAN_TYPE_ID = 2L;
  private static final Long PARENT_LOAN_ID = 10L;
  private static final Long ACCOUNT_ID = 3L;
  private static final BigDecimal OFFER_AMOUNT = new BigDecimal("1000000");
  private static final BigDecimal OFFER_RATE = new BigDecimal("21.5");
  private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("52000");
  private static final int OFFER_DURATION = 24;
  private static final int UPDATED_ROW_COUNT = 1;
  private static final String SQL_ERROR_MESSAGE = "update failed";
  private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);

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

  @InjectMocks
  private LoanDaoImpl dao;

  @BeforeEach
  void setUp() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
  }

  @Test
  void getPendingLoansMapsRows() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, false);
    loanRow(LOAN_ID, USER_ID, LOAN_TYPE_ID, null, LoanStatus.PENDING);

    List<Loan> loans = dao.getPendingLoans();

    assertEquals(UPDATED_ROW_COUNT, loans.size());
    assertEquals(LOAN_ID, loans.getFirst().getLoanId());
    assertEquals(LoanStatus.PENDING.name(), loans.getFirst().getStatus());
    verify(statement).setString(1, LoanStatus.PENDING.name());
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void createOfferBindsManagerCalculatedFields() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    boolean created = dao.createOffer(
        PARENT_LOAN_ID,
        USER_ID,
        LOAN_TYPE_ID,
        ACCOUNT_ID,
        OFFER_AMOUNT,
        OFFER_RATE,
        OFFER_DURATION,
        MONTHLY_PAYMENT
    );

    assertTrue(created);
    verify(statement).setLong(1, USER_ID);
    verify(statement).setLong(2, LOAN_TYPE_ID);
    verify(statement).setLong(3, PARENT_LOAN_ID);
    verify(statement).setLong(4, ACCOUNT_ID);
    verify(statement).setBigDecimal(5, OFFER_AMOUNT);
    verify(statement).setBigDecimal(6, OFFER_RATE);
    verify(statement).setInt(7, OFFER_DURATION);
    verify(statement).setString(8, LoanStatus.OFFERED.name());
    verify(statement).setBigDecimal(9, MONTHLY_PAYMENT);
  }

  @Test
  void acceptOfferActivatesSelectedOfferAndClosesParentRequest() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement, activateStatement, rejectSiblingsStatement, closeParentStatement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    loanRow(LOAN_ID, USER_ID, LOAN_TYPE_ID, PARENT_LOAN_ID, LoanStatus.OFFERED);

    assertTrue(dao.acceptOffer(USER_ID, LOAN_ID));

    verify(connection).setAutoCommit(false);
    verify(activateStatement).setString(1, LoanStatus.ACTIVE.name());
    verify(activateStatement).setLong(3, LOAN_ID);
    verify(rejectSiblingsStatement).setString(1, LoanStatus.REFUSED.name());
    verify(rejectSiblingsStatement).setLong(2, PARENT_LOAN_ID);
    verify(closeParentStatement).setString(1, LoanStatus.CLOSED.name());
    verify(closeParentStatement).setLong(2, PARENT_LOAN_ID);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  void acceptOfferWithoutParentOnlyActivatesOffer() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement, activateStatement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    loanRow(LOAN_ID, USER_ID, LOAN_TYPE_ID, null, LoanStatus.OFFERED);

    assertTrue(dao.acceptOffer(USER_ID, LOAN_ID));

    verify(activateStatement).setString(1, LoanStatus.ACTIVE.name());
    verify(activateStatement).setLong(3, LOAN_ID);
    verify(connection).commit();
  }

  @Test
  void acceptOfferReturnsFalseWhenOfferNotFound() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertEquals(false, dao.acceptOffer(USER_ID, LOAN_ID));

    verify(connection).rollback();
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void getLoanByIdReturnsEmptyWhenNoRowExists() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertTrue(dao.getLoanById(LOAN_ID).isEmpty());
  }

  @Test
  void payLoanWrapsSqlException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException(SQL_ERROR_MESSAGE));

    assertThrows(BankDataAccessException.class, () -> dao.payLoan(LOAN_ID, BigDecimal.TEN));
  }

  private void loanRow(Long loanId, Long userId, Long loanTypeId, Long parentLoanId, LoanStatus status) throws SQLException {
    when(resultSet.getLong("loan_id")).thenReturn(loanId);
    when(resultSet.getLong("user_id")).thenReturn(userId);
    when(resultSet.getLong("loan_type_id")).thenReturn(loanTypeId);
    when(resultSet.getLong("parent_loan_id")).thenReturn(parentLoanId == null ? 0L : parentLoanId);
    when(resultSet.getLong("account_id")).thenReturn(ACCOUNT_ID);
    when(resultSet.wasNull()).thenReturn(parentLoanId == null, false);
    when(resultSet.getBigDecimal("remaining_amount")).thenReturn(OFFER_AMOUNT);
    when(resultSet.getBigDecimal("rate")).thenReturn(OFFER_RATE);
    when(resultSet.getObject("duration")).thenReturn(OFFER_DURATION);
    when(resultSet.getString("status")).thenReturn(status.name());
    when(resultSet.getDate("start_date")).thenReturn(Date.valueOf(START_DATE));
    when(resultSet.getBigDecimal("monthly_payment")).thenReturn(MONTHLY_PAYMENT);
  }
}
