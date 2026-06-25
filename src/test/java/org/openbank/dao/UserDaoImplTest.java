package org.openbank.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.impl.UserDaoImpl;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDaoImplTest {

  private static final Long USER_ID = 9L;
  private static final String NAME = "Aruzhan";
  private static final String SURNAME = "Sadyk";
  private static final String PHONE_NUMBER = "+77001112233";
  private static final String EMAIL = "aru@example.com";
  private static final String CLIENT_ROLE = "CLIENT";
  private static final String PASSWORD_HASH = "hash";
  private static final LocalDate DATE_CREATED = LocalDate.of(2026, 1, 1);
  private static final int UPDATED_ROW_COUNT = 1;

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
    User user = new User(null, NAME, SURNAME, PHONE_NUMBER, EMAIL, CLIENT_ROLE, UserStatus.ACTIVE.name(), DATE_CREATED, null, PASSWORD_HASH);
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    assertTrue(dao.createNewUser(user));

    verify(statement).setString(1, NAME);
    verify(statement).setString(2, SURNAME);
    verify(statement).setString(3, PHONE_NUMBER);
    verify(statement).setString(4, EMAIL);
    verify(statement).setString(8, PASSWORD_HASH);
  }

  @Test
  void getUserByIdMapsResultSet() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
    when(resultSet.getLong("user_id")).thenReturn(USER_ID);
    when(resultSet.getString("name")).thenReturn(NAME);
    when(resultSet.getString("surname")).thenReturn(SURNAME);
    when(resultSet.getString("phone_number")).thenReturn(PHONE_NUMBER);
    when(resultSet.getString("email_address")).thenReturn(EMAIL);
    when(resultSet.getString("role")).thenReturn(CLIENT_ROLE);
    when(resultSet.getString("status")).thenReturn(UserStatus.ACTIVE.name());
    when(resultSet.getDate("date_created")).thenReturn(Date.valueOf(DATE_CREATED));
    when(resultSet.getDate("date_modified")).thenReturn(null);
    when(resultSet.getString("password_hash")).thenReturn(PASSWORD_HASH);

    Optional<User> user = dao.getUserById(USER_ID);

    assertTrue(user.isPresent());
    assertEquals(NAME, user.get().getName());
    assertEquals(PASSWORD_HASH, user.get().getPasswordHash());
    verify(statement).setLong(1, USER_ID);
  }

  @Test
  void existsByEmailAddressReturnsFalseWhenNoRowExists() throws SQLException {
    when(statement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(false);

    assertFalse(dao.existsByEmailAddress(EMAIL));

    verify(statement).setString(1, EMAIL);
  }

  @Test
  void changeStatusOfUserByIdBindsStatusAndUserId() throws SQLException {
    when(statement.executeUpdate()).thenReturn(UPDATED_ROW_COUNT);

    assertTrue(dao.changeStatusOfUserById(USER_ID, UserStatus.DEACTIVATED.name()));

    verify(statement).setString(1, UserStatus.DEACTIVATED.name());
    verify(statement).setLong(2, USER_ID);
  }
}
