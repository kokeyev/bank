package org.openbank.dao.impl;

import org.openbank.dao.CurrencyDao;
import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.Currency;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CurrencyDaoImpl implements CurrencyDao {

  private final ConnectionPool connectionPool;

  public CurrencyDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public Optional<Currency> getCurrencyById(Long currencyId) {
    String sql = """
        select currency_id, name, rate_to_kzt
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
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch currency", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<Currency> getCurrencyByName(String name) {
    String sql = """
        select currency_id, name, rate_to_kzt
        from currencies
        where name = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, name);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch currency by name", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<Currency> getAllCurrencies() {
    List<Currency> currencies = new ArrayList<>();

    String sql = """
        select currency_id, name, rate_to_kzt
        from currencies
        order by name
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            currencies.add(map(resultSet));
          }
        }

        return currencies;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch currency list", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
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

        throw new BankDataAccessException("error.currency.notFound");
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("error.currency.notFound", e);
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

        throw new BankDataAccessException("error.currency.notFound");
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not fetch currency rate", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean updateCurrencyRate(Long currencyId, BigDecimal rateToKzt) {
    String sql = """
        update currencies
        set rate_to_kzt = ?
        where currency_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, rateToKzt);
        statement.setLong(2, currencyId);
        return statement.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Could not update currency rate", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  private Currency map(ResultSet resultSet) throws SQLException {
    return new Currency(
        resultSet.getLong("currency_id"),
        resultSet.getString("name"),
        resultSet.getBigDecimal("rate_to_kzt")
    );
  }
}
