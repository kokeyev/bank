package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.CurrencyDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.Currency;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyDaoImplTest {

  private static final Long CURRENCY_ID = 1L;
  private static final String CURRENCY_NAME = "KZT";
  private static final String SQL_ERROR_MESSAGE = "database down";
  private static final BigDecimal UPDATED_RATE_TO_KZT = new BigDecimal("510.50");
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
  private CurrencyDaoImpl testingInstance;

  @BeforeEach
  void setUp() throws SQLException {
    testingInstance = new CurrencyDaoImpl(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  @Test
  void getCurrencyByNameMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getLong("currency_id")).thenReturn(CURRENCY_ID);
    when(resultSet.getString("name")).thenReturn(CURRENCY_NAME);
    when(resultSet.getBigDecimal("rate_to_kzt")).thenReturn(BigDecimal.ONE);

    Optional<Currency> currency = testingInstance.getCurrencyByName(CURRENCY_NAME);

    assertTrue(currency.isPresent());
    assertEquals(CURRENCY_ID, currency.get().getCurrencyId());
    assertEquals(CURRENCY_NAME, currency.get().getName());
    assertEquals(BigDecimal.ONE, currency.get().getRateToKzt());
    verify(statement).setString(1, CURRENCY_NAME);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void updateCurrencyRateReturnsTrueWhenRowUpdated() throws SQLException {
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    boolean updated = testingInstance.updateCurrencyRate(CURRENCY_ID, UPDATED_RATE_TO_KZT);

    verify(statement).setBigDecimal(1, UPDATED_RATE_TO_KZT);
    verify(statement).setLong(2, CURRENCY_ID);
    assertTrue(updated);
  }

  @Test
  void getCurrencyByIdWrapsSqlException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException(SQL_ERROR_MESSAGE));

    Executable executable = () -> testingInstance.getCurrencyById(CURRENCY_ID);
    assertThrows(BankDataAccessException.class, executable);
    verify(connectionPool).releaseConnection(connection);
  }
}
