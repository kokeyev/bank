package org.author.demo.dao.currency;

import org.author.demo.db.ConnectionPool;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class CurrencyDaoImpl implements CurrencyDao {

  private final ConnectionPool connectionPool;

  public CurrencyDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public String getCurrencyNameById(Long currencyId) {
    String sql = """
        select name
        from currencies
        where currency_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, currencyId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getString("name");
          }
        }

        throw new RuntimeException("Валюта не найдена");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось получить название валюты", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public BigDecimal getCurrencyRateToKztById(Long currencyId) {
    String sql = """
        select rate_to_kzt
        from currencies
        where currency_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, currencyId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getBigDecimal("rate_to_kzt");
          }
        }

        throw new RuntimeException("Валюта не найдена");
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось получить курс валюты", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }
}
