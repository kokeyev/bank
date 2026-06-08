package org.author.demo.dao.user;

import org.author.demo.db.ConnectionPool;
import org.author.demo.model.User;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao{

  private final ConnectionPool connectionPool;

  public UserDaoImpl(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }


  @Override
  public boolean createNewUser(User user) {
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

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось добавить пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<User> getUserByPhoneNumber(String phoneNumber) {
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
            return Optional.of(map(resultSet));
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

  @Override
  public Optional<User> getUserByEmailAddress(String emailAddress) {
    String sql = """
        select name from users where email_address = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, emailAddress);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось найти пользователя по почте", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changePhoneNumberOfUserById(Long user_id, String newPhoneNumber) {
    String sql = """
        update users set phone_number = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newPhoneNumber);
        statement.setLong(2, user_id);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось поменять номер телефона", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changeEmailAddressOfUserById(Long user_id, String newEmailAddress) {
    String sql = """
        update users set email_address = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newEmailAddress);
        statement.setLong(2, user_id);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось поменять почту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changePasswordHashOfUserById(Long user_id, String newPasswordHash) {
    String sql = """
        update users set password_hash = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newPasswordHash);
        statement.setLong(2, user_id);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось поменять пароль", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean deleteUserById(Long user_id) {
    String sql = """
        delete from users where used_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, user_id);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new RuntimeException("Не удалось поменять пароль", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }


  private User map(ResultSet resultSet) throws SQLException {
    User user = new User();

    user.setUserId(resultSet.getLong("user_id"));
    user.setName(resultSet.getString("name"));
    user.setSurname(resultSet.getString("surname"));
    user.setPhoneNumber(resultSet.getString("phone_number"));
    user.setEmailAddress(resultSet.getString("email_address"));
    user.setRole(resultSet.getString("role"));
    user.setStatus(resultSet.getString("status"));
    user.setDateCreated(resultSet.getDate("user_id").toLocalDate());
    user.setPassword_hash(resultSet.getString("password_hash"));

    return user;
  }

}
