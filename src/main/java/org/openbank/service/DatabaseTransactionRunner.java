package org.openbank.service;

import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankTransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs DAO work inside an explicit JDBC transaction.
 *
 * <p>The runner centralizes commit, rollback, auto-commit reset, and exception wrapping so service
 * methods can express multi-step money operations as one callback.</p>
 */
@Component
public class DatabaseTransactionRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTransactionRunner.class);

  private final ConnectionPool connectionPool;
  public DatabaseTransactionRunner(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  /**
   * Executes a callback with a pooled connection and commits only when the callback completes.
   *
   * @param failureMessage message used when a checked SQL failure is wrapped
   * @param callback database work that must commit or roll back as one unit
   * @param <T> callback result type
   * @return value returned by the callback
   * @throws BankTransactionException when a checked SQL error occurs
   * @throws RuntimeException rethrows validation and domain failures from the callback after rollback
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
   * Callback for service operations that need direct access to one transactional connection.
   *
   * @param <T> result type produced by the transaction
   */
  @FunctionalInterface
  public interface TransactionCallback<T> {

    /**
     * Performs database work using the connection managed by {@link DatabaseTransactionRunner}.
     *
     * @param connection connection with auto-commit disabled
     * @return result passed back from {@link DatabaseTransactionRunner#run(String, TransactionCallback)}
     * @throws SQLException when DAO work fails at JDBC level
     */
    T execute(Connection connection) throws SQLException;
  }
}
