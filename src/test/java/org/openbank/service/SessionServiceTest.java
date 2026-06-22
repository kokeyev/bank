package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.openbank.service.impl.CurrentUserServiceImpl;
import org.openbank.service.impl.StaffSessionServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

  private static final Long USER_ID = 5L;
  private static final Long ADMIN_ID = 7L;
  private static final Long MANAGER_ID = 8L;
  private static final String USER_NAME = "A";
  private static final String USER_SURNAME = "B";
  private static final String PHONE_NUMBER = "+7";
  private static final String CLIENT_EMAIL = "a@b.kz";
  private static final String ADMIN_EMAIL = "admin@b.kz";
  private static final String MANAGER_EMAIL = "m@b.kz";
  private static final String CLIENT_ROLE = "CLIENT";
  private static final String ADMIN_ROLE = "ADMIN";
  private static final String MANAGER_ROLE = "MANAGER";
  private static final String ACTIVE_STATUS = "ACTIVE";
  private static final String PASSWORD_HASH = "hash";

  @Test
  void currentUserServiceRemovesStaleSessionUser() {
    UserService userService = mock(UserService.class);
    HttpSession session = mock(HttpSession.class);
    CurrentUserService currentUserService = new CurrentUserServiceImpl(userService);
    when(session.getAttribute(SessionKeys.CURRENT_USER_ID)).thenReturn(USER_ID);
    when(userService.getUserById(USER_ID)).thenReturn(Optional.empty());

    assertFalse(currentUserService.isLoggedIn(session));
    verify(session).removeAttribute(SessionKeys.CURRENT_USER_ID);
  }

  @Test
  void currentUserServiceStoresUserIdOnLogin() {
    HttpSession session = mock(HttpSession.class);
    CurrentUserService currentUserService = new CurrentUserServiceImpl(mock(UserService.class));
    User user = new User(USER_ID, USER_NAME, USER_SURNAME, PHONE_NUMBER, CLIENT_EMAIL, CLIENT_ROLE, ACTIVE_STATUS, LocalDate.now(), null, PASSWORD_HASH);

    currentUserService.login(session, user);

    verify(session).setAttribute(SessionKeys.CURRENT_USER_ID, USER_ID);
  }

  @Test
  void staffSessionServiceStoresAdminAndManagerSessions() {
    StaffSessionService staffSessionService = new StaffSessionServiceImpl();
    HttpSession session = mock(HttpSession.class);
    User admin = new User(ADMIN_ID, USER_NAME, USER_SURNAME, PHONE_NUMBER, ADMIN_EMAIL, ADMIN_ROLE, ACTIVE_STATUS, LocalDate.now(), null, PASSWORD_HASH);
    User manager = new User(MANAGER_ID, USER_NAME, USER_SURNAME, PHONE_NUMBER, MANAGER_EMAIL, MANAGER_ROLE, ACTIVE_STATUS, LocalDate.now(), null, PASSWORD_HASH);

    staffSessionService.loginAdmin(session, admin);
    staffSessionService.loginManager(session, manager);

    verify(session).setAttribute(SessionKeys.CURRENT_ADMIN_ID, ADMIN_ID);
    verify(session).setAttribute(SessionKeys.CURRENT_MANAGER_ID, MANAGER_ID);
  }
}
