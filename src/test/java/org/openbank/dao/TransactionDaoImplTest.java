package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.TransactionDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Transaction;
import org.openbank.model.status.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionDaoImplTest {

  private static final Long TRANSACTION_ID = 11L;
  private static final Long SENDER_ACCOUNT_ID = 3L;
  private static final Long RECEIVER_ACCOUNT_ID = 4L;
  private static final Long USER_ID = 7L;
  private static final Long CURRENCY_ID = 1L;
  private static final BigDecimal AMOUNT = new BigDecimal("2500");
  private static final BigDecimal FEE = new BigDecimal("25");
  private static final String MESSAGE = "Test payment";
  private static final String SQL_ERROR_MESSAGE = "insert failed";
  private static final LocalDateTime TRANSACTION_DATE = LocalDateTime.of(2026, 1, 1, 12, 30);
  private static final int UPDATED_ROW_COUNT = 1;
  private static final int LIMIT = 5;
  private static final int OFFSET = 10;

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  private TransactionDaoImpl dao;

  @BeforeEach
  void setUp() {
    dao = new TransactionDaoImpl(connectionPool);
  }

  @Test
  void createNewTransactionBindsAllFields() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    assertTrue(dao.createNewTransaction(SENDER_ACCOUNT_ID, RECEIVER_ACCOUNT_ID, TRANSACTION_DATE, AMOUNT, CURRENCY_ID, FEE, MESSAGE, TransactionType.CARD_TRANSFER.name()));

    verify(statement).setObject(1, SENDER_ACCOUNT_ID);
    verify(statement).setObject(2, RECEIVER_ACCOUNT_ID);
    verify(statement).setTimestamp(3, Timestamp.valueOf(TRANSACTION_DATE));
    verify(statement).setBigDecimal(4, AMOUNT);
    verify(statement).setLong(5, CURRENCY_ID);
    verify(statement).setBigDecimal(6, FEE);
    verify(statement).setString(7, MESSAGE);
    verify(statement).setString(8, TransactionType.CARD_TRANSFER.name());
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void getTransactionsByUserIdMapsRowsAndBindsPaging() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true, false);
    transactionRow();

    List<Transaction> transactions = dao.getTransactionsByUserId(USER_ID, LIMIT, OFFSET);

    assertEquals(UPDATED_ROW_COUNT, transactions.size());
    assertEquals(TRANSACTION_ID, transactions.getFirst().getTransactionId());
    assertEquals(SENDER_ACCOUNT_ID, transactions.getFirst().getSenderAccountId());
    assertEquals(TransactionType.CARD_TRANSFER.name(), transactions.getFirst().getTransactionType());
    verify(statement).setLong(1, USER_ID);
    verify(statement).setLong(2, USER_ID);
    verify(statement).setInt(3, LIMIT);
    verify(statement).setInt(4, OFFSET);
  }

  @Test
  void createNewTransactionWrapsSqlException() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeUpdate()).thenThrow(new SQLException(SQL_ERROR_MESSAGE));

    assertThrows(BankDataAccessException.class, () -> dao.createNewTransaction(SENDER_ACCOUNT_ID, RECEIVER_ACCOUNT_ID, TRANSACTION_DATE, AMOUNT, CURRENCY_ID, FEE, MESSAGE, TransactionType.CARD_TRANSFER.name()));
  }

  private void transactionRow() throws SQLException {
    when(resultSet.getLong("transaction_id")).thenReturn(TRANSACTION_ID);
    when(resultSet.getLong("sender_account_id")).thenReturn(SENDER_ACCOUNT_ID);
    when(resultSet.getLong("receiver_account_id")).thenReturn(RECEIVER_ACCOUNT_ID);
    when(resultSet.wasNull()).thenReturn(false, false);
    when(resultSet.getTimestamp("transaction_date")).thenReturn(Timestamp.valueOf(TRANSACTION_DATE));
    when(resultSet.getBigDecimal("amount")).thenReturn(AMOUNT);
    when(resultSet.getLong("currency_id")).thenReturn(CURRENCY_ID);
    when(resultSet.getBigDecimal("fee")).thenReturn(FEE);
    when(resultSet.getString("message")).thenReturn(MESSAGE);
    when(resultSet.getString("transaction_type")).thenReturn(TransactionType.CARD_TRANSFER.name());
  }
}
