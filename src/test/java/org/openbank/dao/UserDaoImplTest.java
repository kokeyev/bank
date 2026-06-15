package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.user.UserDaoImpl;
import org.openbank.db.ConnectionPool;
import org.openbank.model.User;
import org.openbank.model.status.UserStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDaoImplTest {

  @Mock
  private ConnectionPool connectionPool;

  @Mock
  private Connection connection;

  @Mock
  private PreparedStatement statement;

  @Mock
  private ResultSet resultSet;

  private UserDaoImpl dao;

  @BeforeEach
  void setUp() throws SQLException {
    dao = new UserDaoImpl(connectionPool);
    when(connectionPool.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
  }

  @Test
  void createNewUserBindsPasswordHashAndContactFields() throws SQLException {
    User user = new User(null, "Aruzhan", "Sadyk", "+77001112233", "aru@example.com", "CLIENT", UserStatus.ACTIVE.name(), LocalDate.of(2026, 1, 1), null, "hash");
    when(statement.executeUpdate()).thenReturn(1);

    assertTrue(dao.createNewUser(user));

    verify(statement).setString(1, "Aruzhan");
    verify(statement).setString(2, "Sadyk");
    verify(statement).setString(3, "+77001112233");
    verify(statement).setString(4, "aru@example.com");
    verify(statement).setString(8, "hash");
  }

  @Test
  void getUserByIdMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getLong("user_id")).thenReturn(9L);
    when(resultSet.getString("name")).thenReturn("Aruzhan");
    when(resultSet.getString("surname")).thenReturn("Sadyk");
    when(resultSet.getString("phone_number")).thenReturn("+77001112233");
    when(resultSet.getString("email_address")).thenReturn("aru@example.com");
    when(resultSet.getString("role")).thenReturn("CLIENT");
    when(resultSet.getString("status")).thenReturn(UserStatus.ACTIVE.name());
    when(resultSet.getDate("date_created")).thenReturn(Date.valueOf(LocalDate.of(2026, 1, 1)));
    when(resultSet.getDate("date_modified")).thenReturn(null);
    when(resultSet.getString("password_hash")).thenReturn("hash");

    Optional<User> user = dao.getUserById(9L);

    assertTrue(user.isPresent());
    assertEquals("Aruzhan", user.get().getName());
    assertEquals("hash", user.get().getPasswordHash());
    verify(statement).setLong(1, 9L);
  }
}
