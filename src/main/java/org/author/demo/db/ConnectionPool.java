package org.author.demo.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPool {

  private String url;
  private String username;
  private String password;
  private int poolSize;

  private List<Connection> availableConnections;
  private List<Connection> usedConnections;

  public ConnectionPool(String url, String username, String password, int poolSize) {
    this.url = url;
    this.username = username;
    this.password = password;
    this.poolSize = poolSize;

    this.availableConnections = new ArrayList<>();
    this.usedConnections = new ArrayList<>();

    try {
      Class.forName("org.postgresql.Driver");

      for (int i = 0; i < poolSize; i++) {
        Connection connection = DriverManager.getConnection(url, username, password);
        availableConnections.add(connection);
      }
    } catch (ClassNotFoundException | SQLException e) {
      throw new RuntimeException("Не удалось создать пул соединений", e);
    }
  }

  public synchronized Connection getConnection() {
    while (availableConnections.isEmpty()) {
      try {
        wait();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException("Ожидание свободного соединения было прервано", e);
      }
    }

    Connection connection = availableConnections.removeFirst();
    usedConnections.add(connection);

    return connection;
  }

  public synchronized void releaseConnection(Connection connection) {
    if (connection == null) {
      return;
    }

    if (usedConnections.remove(connection)) {
      availableConnections.add(connection);
      notifyAll();
    }
  }

  public synchronized void closeAllConnections() {
    closeConnections(availableConnections);
    closeConnections(usedConnections);

    availableConnections.clear();
    usedConnections.clear();
  }

  private void closeConnections(List<Connection> connections) {
    for (Connection connection : connections) {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException e) {
          System.err.println("Не удалось закрыть соединение: " + e.getMessage());
        }
      }
    }
  }

}
