package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.AccountDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Account;
import org.openbank.model.status.AccountStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDaoImplTest {

  private static final Long ACCOUNT_ID = 5L;
  private static final Long USER_ID = 7L;
  private static final Long CURRENCY_ID = 1L;
  private static final String CARD_NUMBER = "4000000000000002";
  private static final String CVV = "123";
  private static final String ACCOUNT_NAME = "Main";
  private static final String SQL_ERROR_MESSAGE = "write failed";
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2030, 1, 1);
  private static final BigDecimal BALANCE = new BigDecimal("1500");
  private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("100000");
  private static final int UPDATED_ROW_COUNT = 1;

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  @InjectMocks
  private AccountDaoImpl testingInstance;

  @BeforeEach
  void setUp() throws SQLException {
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  @Test
  void createNewAccountBindsAllFields() throws SQLException {
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    boolean result = testingInstance.createNewAccount(
        USER_ID,
        CARD_NUMBER,
        CVV,
        EXPIRY_DATE,
        BigDecimal.ZERO,
        CURRENCY_ID,
        AccountStatus.PENDING,
        TRANSACTION_LIMIT,
        ACCOUNT_NAME,
        false
    );

    verify(statement).setLong(1, USER_ID);
    verify(statement).setString(2, CARD_NUMBER);
    verify(statement).setString(3, CVV);
    verify(statement).setDate(4, Date.valueOf(EXPIRY_DATE));
    verify(statement).setBigDecimal(5, BigDecimal.ZERO);
    verify(statement).setLong(6, CURRENCY_ID);
    verify(statement).setString(7, AccountStatus.PENDING.name());
    verify(statement).setBigDecimal(8, TRANSACTION_LIMIT);
    verify(statement).setString(9, ACCOUNT_NAME);
    verify(statement).setBoolean(10, false);
    assertTrue(result);
  }

  @Test
  void getAccountByIdMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    accountRow();

    Optional<Account> account = testingInstance.getAccountById(ACCOUNT_ID);

    assertTrue(account.isPresent());
    assertEquals(ACCOUNT_ID, account.get().getAccountId());
    assertEquals(CARD_NUMBER, account.get().getCardNumber());
    assertEquals(AccountStatus.ACTIVE.name(), account.get().getStatus());
    verify(statement).setLong(1, ACCOUNT_ID);
  }

  @Test
  void withdrawWrapsSqlException() throws SQLException {
    when(statement.executeUpdate()).thenThrow(new SQLException(SQL_ERROR_MESSAGE));

    Executable executable = () -> testingInstance.withdraw(ACCOUNT_ID, BigDecimal.TEN);

    assertThrows(BankDataAccessException.class, executable);
  }

  private void accountRow() throws SQLException {
    when(resultSet.getLong("account_id")).thenReturn(ACCOUNT_ID);
    when(resultSet.getLong("user_id")).thenReturn(USER_ID);
    when(resultSet.getString("card_number")).thenReturn(CARD_NUMBER);
    when(resultSet.getString("cvv")).thenReturn(CVV);
    when(resultSet.getDate("expiry_date")).thenReturn(Date.valueOf(EXPIRY_DATE));
    when(resultSet.getBigDecimal("balance")).thenReturn(BALANCE);
    when(resultSet.getLong("currency_id")).thenReturn(CURRENCY_ID);
    when(resultSet.getString("status")).thenReturn(AccountStatus.ACTIVE.name());
    when(resultSet.getBigDecimal("transaction_limit")).thenReturn(TRANSACTION_LIMIT);
    when(resultSet.getString("name")).thenReturn(ACCOUNT_NAME);
    when(resultSet.getBoolean("is_main")).thenReturn(true);
  }
}
