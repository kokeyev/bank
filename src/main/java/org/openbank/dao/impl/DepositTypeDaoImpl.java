package org.openbank.dao.impl;

import org.openbank.dao.DepositTypeDao;
import org.openbank.exception.BankDataAccessException;

import org.openbank.db.ConnectionPool;
import org.openbank.model.DepositType;
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
public class DepositTypeDaoImpl implements DepositTypeDao {

  private final ConnectionPool connectionPool;

  public DepositTypeDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  @Override
  public boolean createNewDepositType(String name, BigDecimal rate, Integer duration, Boolean withdrawal, BigDecimal minimumAmount, Long currencyId) {
    String sql = """
        insert into deposit_types (name, rate, duration, withdrawal, minimum_amount, currency_id)
        values (?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, name);
        statement.setBigDecimal(2, rate);
        statement.setInt(3, duration);
        statement.setBoolean(4, withdrawal);
        statement.setBigDecimal(5, minimumAmount);
        statement.setLong(6, currencyId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось создать тип депозита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<DepositType> getDepositTypeById(Long depositTypeId) {
    String sql = """
        select deposit_type_id, name, rate, duration, withdrawal, minimum_amount, currency_id
        from deposit_types
        where deposit_type_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, depositTypeId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }

        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить тип депозита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<DepositType> getAllDepositTypes() {
    List<DepositType> depositTypes = new ArrayList<>();

    String sql = """
        select deposit_type_id, name, rate, duration, withdrawal, minimum_amount, currency_id
        from deposit_types
        order by name
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            depositTypes.add(map(resultSet));
          }
        }

        return depositTypes;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить типы депозитов", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changeRateOfDepositType(Long depositTypeId, BigDecimal newRate) {
    String sql = """
        update deposit_types
        set rate = ?
        where deposit_type_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setBigDecimal(1, newRate);
        statement.setLong(2, depositTypeId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить ставку типа депозита", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  private DepositType map(ResultSet resultSet) throws SQLException {
    return new DepositType(
        resultSet.getLong("deposit_type_id"),
        resultSet.getString("name"),
        resultSet.getBigDecimal("rate"),
        resultSet.getInt("duration"),
        resultSet.getBoolean("withdrawal"),
        resultSet.getBigDecimal("minimum_amount"),
        resultSet.getLong("currency_id")
    );
  }
}
