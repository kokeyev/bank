package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankTransactionException;
import org.openbank.service.impl.DatabaseTransactionRunnerImpl;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseTransactionRunnerTest {

  private static final String FAILURE_MESSAGE = "failed";
  private static final String SUCCESS_RESULT = "ok";
  private static final String SQL_ERROR_MESSAGE = "broken";
  private static final String RUNTIME_ERROR_MESSAGE = "bad state";

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  private DatabaseTransactionRunner runner;

  @BeforeEach
  void setUp() throws SQLException {
    runner = new DatabaseTransactionRunnerImpl(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
  }

  @Test
  void runCommitsAndReleasesConnectionOnSuccess() throws SQLException {
    String result = runner.run(FAILURE_MESSAGE, activeConnection -> SUCCESS_RESULT);

    assertEquals(SUCCESS_RESULT, result);
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void runRollsBackAndWrapsSqlException() throws SQLException {
    assertThrows(BankTransactionException.class, () -> runner.run(FAILURE_MESSAGE, activeConnection -> {
      throw new SQLException(SQL_ERROR_MESSAGE);
    }));

    verify(connection).rollback();
    verify(connection).setAutoCommit(true);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void runRollsBackAndRethrowsRuntimeException() throws SQLException {
    assertThrows(IllegalStateException.class, () -> runner.run(FAILURE_MESSAGE, activeConnection -> {
      throw new IllegalStateException(RUNTIME_ERROR_MESSAGE);
    }));

    verify(connection).rollback();
    verify(connectionPool).releaseConnection(connection);
  }
}
