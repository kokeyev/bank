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
    LocalDate expiryDate = LocalDate.of(2030, 1, 1);
    when(statement.executeUpdate()).thenReturn(1);

    boolean result = testingInstance.createNewAccount(
        7L,
        "4000000000000002",
        "123",
        expiryDate,
        BigDecimal.ZERO,
        1L,
        AccountStatus.PENDING,
        new BigDecimal("100000"),
        "Main",
        false
    );

    verify(statement).setLong(1, 7L);
    verify(statement).setString(2, "4000000000000002");
    verify(statement).setString(3, "123");
    verify(statement).setDate(4, Date.valueOf(expiryDate));
    verify(statement).setBigDecimal(5, BigDecimal.ZERO);
    verify(statement).setLong(6, 1L);
    verify(statement).setString(7, AccountStatus.PENDING.name());
    verify(statement).setBigDecimal(8, new BigDecimal("100000"));
    verify(statement).setString(9, "Main");
    verify(statement).setBoolean(10, false);
    assertTrue(result);
  }

  @Test
  void getAccountByIdMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    accountRow();

    Optional<Account> account = testingInstance.getAccountById(5L);

    assertTrue(account.isPresent());
    assertEquals(5L, account.get().getAccountId());
    assertEquals("4000000000000002", account.get().getCardNumber());
    assertEquals(AccountStatus.ACTIVE.name(), account.get().getStatus());
    verify(statement).setLong(1, 5L);
  }

  @Test
  void withdrawWrapsSqlException() throws SQLException {
    when(statement.executeUpdate()).thenThrow(new SQLException("write failed"));

    Executable executable = () -> testingInstance.withdraw(5L, BigDecimal.TEN);

    assertThrows(BankDataAccessException.class, executable);
  }

  private void accountRow() throws SQLException {
    when(resultSet.getLong("account_id")).thenReturn(5L);
    when(resultSet.getLong("user_id")).thenReturn(7L);
    when(resultSet.getString("card_number")).thenReturn("4000000000000002");
    when(resultSet.getString("cvv")).thenReturn("123");
    when(resultSet.getDate("expiry_date")).thenReturn(Date.valueOf(LocalDate.of(2030, 1, 1)));
    when(resultSet.getBigDecimal("balance")).thenReturn(new BigDecimal("1500"));
    when(resultSet.getLong("currency_id")).thenReturn(1L);
    when(resultSet.getString("status")).thenReturn(AccountStatus.ACTIVE.name());
    when(resultSet.getBigDecimal("transaction_limit")).thenReturn(new BigDecimal("100000"));
    when(resultSet.getString("name")).thenReturn("Main");
    when(resultSet.getBoolean("is_main")).thenReturn(true);
  }
}
