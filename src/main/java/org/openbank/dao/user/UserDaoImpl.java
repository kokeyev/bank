package org.openbank.dao.user;

import org.openbank.db.ConnectionPool;
import org.openbank.exception.BankDataAccessException;
import org.openbank.model.User;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl implements UserDao {

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
        statement.setString(8, user.getPasswordHash());

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось добавить пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<User> getUserById(Long userId) {
    String sql = """
        select user_id, name, surname, phone_number, email_address, role, status, date_created, date_modified, password_hash
        from users
        where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);
        try (ResultSet resultSet = statement.executeQuery()) {
          if (resultSet.next()) {
            return Optional.of(map(resultSet));
          }
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось найти пользователя по id", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<User> getUserByPhoneNumber(String phoneNumber) {
    String sql = """
        select user_id, name, surname, phone_number, email_address, role, status, date_created, date_modified, password_hash
        from users
        where phone_number = ?
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
      throw new BankDataAccessException("Не удалось найти пользователя по номеру телефона", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public Optional<User> getUserByEmailAddress(String emailAddress) {
    String sql = """
        select user_id, name, surname, phone_number, email_address, role, status, date_created, date_modified, password_hash
        from users
        where email_address = ?
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
      throw new BankDataAccessException("Не удалось найти пользователя по почте", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public List<User> getUsersByRoleAndStatus(String role, String status) {
    List<User> users = new ArrayList<>();

    String sql = """
        select user_id, name, surname, phone_number, email_address, role, status, date_created, date_modified, password_hash
        from users
        where role = ? and status = ?
        order by user_id desc
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, role);
        statement.setString(2, status);
        try (ResultSet resultSet = statement.executeQuery()) {
          while (resultSet.next()) {
            users.add(map(resultSet));
          }
        }
      }
      return users;
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось получить пользователей по роли", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean existsByPhoneNumber(String phoneNumber) {
    String sql = """
        select 1
        from users
        where phone_number = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, phoneNumber);
        try (ResultSet resultSet = statement.executeQuery()) {
          return resultSet.next();
        }
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось проверить номер телефона", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean existsByEmailAddress(String emailAddress) {
    String sql = """
        select 1
        from users
        where email_address = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, emailAddress);
        try (ResultSet resultSet = statement.executeQuery()) {
          return resultSet.next();
        }
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось проверить почту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changePhoneNumberOfUserById(Long userId, String newPhoneNumber) {
    String sql = """
        update users set phone_number = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newPhoneNumber);
        statement.setLong(2, userId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось поменять номер телефона", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changeEmailAddressOfUserById(Long userId, String newEmailAddress) {
    String sql = """
        update users set email_address = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newEmailAddress);
        statement.setLong(2, userId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось поменять почту", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changePasswordHashOfUserById(Long userId, String newPasswordHash) {
    String sql = """
        update users set password_hash = ? where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, newPasswordHash);
        statement.setLong(2, userId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось поменять пароль", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean changeStatusOfUserById(Long userId, String status) {
    String sql = """
        update users set status = ?, date_modified = current_date where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, status);
        statement.setLong(2, userId);

        return statement.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось изменить статус пользователя", e);
    } finally {
      connectionPool.releaseConnection(connection);
    }
  }

  @Override
  public boolean deleteUserById(Long userId) {
    String sql = """
        delete from users where user_id = ?
        """;

    Connection connection = null;

    try {
      connection = connectionPool.getConnection();
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setLong(1, userId);

        int rowsAffected = statement.executeUpdate();
        return rowsAffected > 0;
      }
    } catch (SQLException e) {
      throw new BankDataAccessException("Не удалось поменять пароль", e);
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
    user.setDateCreated(resultSet.getDate("date_created").toLocalDate());
    Date dateModified = resultSet.getDate("date_modified");
    user.setDateModified(dateModified == null ? null : dateModified.toLocalDate());
    user.setPasswordHash(resultSet.getString("password_hash"));

    return user;
  }

}
