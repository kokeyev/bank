package org.author.demo.dao.user;

import org.author.demo.db.ConnectionPool;
import org.author.demo.model.User;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;

@Repository
public class UserDaoImpl {

  private final ConnectionPool connectionPool;

  public UserDao(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  public void createNewUser(User user) {
    String sql = """
        insert into users (name, surname, phone_number, email_address, role, status, date_created, password_hash)
        values (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, user.getName());
        statement.setString(2, user.getSurname());
        statement.setString(3, user.getPhoneNumber());
        statement.setString(4, user.getEmailAddress());
        statement.setString(5, user.getRole());
        statement.setString(6, user.getStatus());
        statement.setDate(7, Date.valueOf(user.getDateCreated()));
        statement.setString(8, user.getPassword_hash());

        statement.executeUpdate();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось сохранить пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }

  }

  public Optional<String> findByPhoneNumber(String phoneNumber) {
    String sql = """
        select name from users where phone_number = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, phoneNumber);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(resultSet.getString("name"));
          }
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось найти пользователя по номеру телефона", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

}
