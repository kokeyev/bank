package org.openbank.service;

import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides database transaction runner operations.
 */
@Component
public class DatabaseTransactionRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTransactionRunner.class);

  private final ConnectionPool connectionPool;

  /**
   * Handles database transaction runner.
   */
  public DatabaseTransactionRunner(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  /**
   * Handles run.
   */
  public <T> T run(String failureMessage, TransactionCallback<T> callback) {
    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      connection.setAutoCommit(false);

      T result = callback.execute(connection);
      connection.commit();
      return result;
    } catch (SQLException | RuntimeException e) {
      rollback(connection);
      if (e instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new BankTransactionException(failureMessage, e);
    } finally {
      resetAutoCommit(connection);
      connectionPool.releaseConnection(connection);
    }
  }

  private void rollback(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.rollback();
    } catch (SQLException e) {
      LOGGER.warn("Не удалось откатить транзакцию", e);
    }
  }

  private void resetAutoCommit(Connection connection) {
    if (connection == null) {
      return;
    }

    try {
      connection.setAutoCommit(true);
    } catch (SQLException e) {
      LOGGER.warn("Не удалось вернуть auto-commit после транзакции", e);
    }
  }

  /**
   * Defines the transaction callback contract.
   */
  @FunctionalInterface
  public interface TransactionCallback<T> {
    T execute(Connection connection) throws SQLException;
  }
}
