package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.DepositDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Deposit;
import org.openbank.model.status.DepositStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositDaoImplTest {

  private static final Long DEPOSIT_ID = 9L;
  private static final Long USER_ID = 7L;
  private static final Long DEPOSIT_TYPE_ID = 5L;
  private static final BigDecimal CURRENT_AMOUNT = new BigDecimal("1200");
  private static final BigDecimal TOP_UP_AMOUNT = new BigDecimal("100");
  private static final String SQL_ERROR_MESSAGE = "update failed";
  private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
  private static final int UPDATED_ROW_COUNT = 1;

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  private DepositDaoImpl dao;

  @BeforeEach
  void setUp() {
    dao = new DepositDaoImpl(connectionPool);
  }

  @Test
  void createDepositBindsAllFields() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    assertTrue(dao.createDeposit(connection, USER_ID, DEPOSIT_TYPE_ID, true, false, DepositStatus.PENDING, START_DATE, CURRENT_AMOUNT));

    verify(statement).setLong(1, USER_ID);
    verify(statement).setLong(2, DEPOSIT_TYPE_ID);
    verify(statement).setBoolean(3, true);
    verify(statement).setBoolean(4, false);
    verify(statement).setString(5, DepositStatus.PENDING.name());
    verify(statement).setDate(6, Date.valueOf(START_DATE));
    verify(statement).setBigDecimal(7, CURRENT_AMOUNT);
  }

  @Test
  void getDepositByIdMapsResultSet() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    depositRow();

    Optional<Deposit> deposit = dao.getDepositById(DEPOSIT_ID);

    assertTrue(deposit.isPresent());
    assertEquals(DEPOSIT_ID, deposit.get().getDepositId());
    assertEquals(DepositStatus.ACTIVE.name(), deposit.get().getStatus());
    assertEquals(CURRENT_AMOUNT, deposit.get().getCurrentAmount());
    verify(statement).setLong(1, DEPOSIT_ID);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void getDepositsByStatusMapsRows() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, false);
    depositRow();

    List<Deposit> deposits = dao.getDepositsByStatus(DepositStatus.ACTIVE);

    assertEquals(UPDATED_ROW_COUNT, deposits.size());
    assertEquals(DEPOSIT_ID, deposits.getFirst().getDepositId());
    verify(statement).setString(1, DepositStatus.ACTIVE.name());
  }

  @Test
  void topUpDepositWrapsSqlException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException(SQL_ERROR_MESSAGE));

    assertThrows(BankDataAccessException.class, () -> dao.topUpDeposit(connection, DEPOSIT_ID, TOP_UP_AMOUNT));
  }

  private void depositRow() throws SQLException {
    when(resultSet.getLong("deposit_id")).thenReturn(DEPOSIT_ID);
    when(resultSet.getLong("user_id")).thenReturn(USER_ID);
    when(resultSet.getLong("deposit_type_id")).thenReturn(DEPOSIT_TYPE_ID);
    when(resultSet.getBoolean("reinvest_interest")).thenReturn(true);
    when(resultSet.getBoolean("auto_renewal")).thenReturn(false);
    when(resultSet.getString("status")).thenReturn(DepositStatus.ACTIVE.name());
    when(resultSet.getDate("start_date")).thenReturn(Date.valueOf(START_DATE));
    when(resultSet.getBigDecimal("current_amount")).thenReturn(CURRENT_AMOUNT);
  }
}
