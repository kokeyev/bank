package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionServiceTest {

  @Test
  void currentUserServiceRemovesStaleSessionUser() {
    UserService userService = mock(UserService.class);
    HttpSession session = mock(HttpSession.class);
    CurrentUserService currentUserService = new CurrentUserService(userService);
    when(session.getAttribute(SessionKeys.CURRENT_USER_ID)).thenReturn(5L);
    when(userService.getUserById(5L)).thenReturn(Optional.empty());

    assertFalse(currentUserService.isLoggedIn(session));
    verify(session).removeAttribute(SessionKeys.CURRENT_USER_ID);
  }

  @Test
  void currentUserServiceStoresUserIdOnLogin() {
    HttpSession session = mock(HttpSession.class);
    CurrentUserService currentUserService = new CurrentUserService(mock(UserService.class));
    User user = new User(5L, "A", "B", "+7", "a@b.kz", "CLIENT", "ACTIVE", LocalDate.now(), null, "hash");

    currentUserService.login(session, user);

    verify(session).setAttribute(SessionKeys.CURRENT_USER_ID, 5L);
  }

  @Test
  void staffSessionServiceHandlesAdminAndManagerSessions() {
    StaffSessionService staffSessionService = new StaffSessionService();
    HttpSession session = mock(HttpSession.class);
    User manager = new User(8L, "M", "B", "+7", "m@b.kz", "MANAGER", "ACTIVE", LocalDate.now(), null, "hash");

    assertTrue(staffSessionService.loginAdmin(session, "admin", "12345"));
    staffSessionService.loginManager(session, manager);

    verify(session).setAttribute(SessionKeys.ADMIN_LOGGED_IN, true);
    verify(session).setAttribute(SessionKeys.CURRENT_MANAGER_ID, 8L);
  }
}
