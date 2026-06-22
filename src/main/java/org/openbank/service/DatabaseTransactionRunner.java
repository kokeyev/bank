package org.openbank.service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Defines explicit JDBC transaction execution.
 */
public interface DatabaseTransactionRunner {

  /** Runs callback work inside one committed or rolled-back transaction. */
  <T> T run(String failureMessage, TransactionCallback<T> callback);

  /**
   * Defines database work executed with one transactional connection.
   */
  @FunctionalInterface
  interface TransactionCallback<T> {

    /** Performs database work using the provided connection. */
    T execute(Connection connection) throws SQLException;
  }
}
