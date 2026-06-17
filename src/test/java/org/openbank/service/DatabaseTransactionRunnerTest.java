package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankTransactionException;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseTransactionRunnerTest {

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  private DatabaseTransactionRunner runner;

  @BeforeEach
  void setUp() throws SQLException {
    runner = new DatabaseTransactionRunner(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
  }

  @Test
  void runCommitsAndReleasesConnectionOnSuccess() throws SQLException {
    String result = runner.run("failed", activeConnection -> "ok");

    assertEquals("ok", result);
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void runRollsBackAndWrapsSqlException() throws SQLException {
    assertThrows(BankTransactionException.class, () -> runner.run("failed", activeConnection -> {
      throw new SQLException("broken");
    }));

    verify(connection).rollback();
    verify(connection).setAutoCommit(true);
    verify(connectionPool).releaseConnection(connection);
  }

  @Test
  void runRollsBackAndRethrowsRuntimeException() throws SQLException {
    assertThrows(IllegalStateException.class, () -> runner.run("failed", activeConnection -> {
      throw new IllegalStateException("bad state");
    }));

    verify(connection).rollback();
    verify(connectionPool).releaseConnection(connection);
  }
}
