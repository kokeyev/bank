package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.currency.CurrencyDaoImpl;
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

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  private CurrencyDaoImpl dao;

  @BeforeEach
  void setUp() throws SQLException {
    dao = new CurrencyDaoImpl(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  @Test
  void getCurrencyByNameMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getLong("currency_id")).thenReturn(1L);
    when(resultSet.getString("name")).thenReturn("KZT");
    when(resultSet.getBigDecimal("rate_to_kzt")).thenReturn(BigDecimal.ONE);

    Optional<Currency> currency = dao.getCurrencyByName("KZT");

    assertTrue(currency.isPresent());
    assertEquals(1L, currency.get().getCurrencyId());
    assertEquals("KZT", currency.get().getName());
    assertEquals(BigDecimal.ONE, currency.get().getRateToKzt());
    verify(statement).setString(1, "KZT");
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void updateCurrencyRateReturnsTrueWhenRowUpdated() throws SQLException {
    when(statement.executeUpdate()).thenReturn(1);

    boolean updated = dao.updateCurrencyRate(1L, new BigDecimal("510.50"));

    assertTrue(updated);
    verify(statement).setBigDecimal(1, new BigDecimal("510.50"));
    verify(statement).setLong(2, 1L);
  }

  @Test
  void getCurrencyByIdWrapsSqlException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("database down"));

    assertThrows(BankDataAccessException.class, () -> dao.getCurrencyById(1L));
    verify(connectionPool).releaseConnection(connection);
  }
}
