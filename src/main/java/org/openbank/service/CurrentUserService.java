package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Provides current user service operations.
 */
@Service
public class CurrentUserService {

  private final UserService userService;

  /**
   * Handles current user service.
   */
  public CurrentUserService(UserService userService) {
    this.userService = userService;
  }

  /**
   * Handles get current user id.
   */
  public Optional<Long> getCurrentUserId(HttpSession session) {
    Object currentUserId = session.getAttribute(SessionKeys.CURRENT_USER_ID);

    if (currentUserId instanceof Long userId) {
      return Optional.of(userId);
    }

    return Optional.empty();
  }

  /**
   * Handles get current user.
   */
  public Optional<User> getCurrentUser(HttpSession session) {
    Optional<Long> currentUserId = getCurrentUserId(session);

    if (currentUserId.isEmpty()) {
      return Optional.empty();
    }

    Optional<User> currentUser = userService.getUserById(currentUserId.get());

    if (currentUser.isEmpty()) {
      session.removeAttribute(SessionKeys.CURRENT_USER_ID);
    }

    return currentUser;
  }

  /**
   * Handles is logged in.
   */
  public boolean isLoggedIn(HttpSession session) {
    return getCurrentUser(session).isPresent();
  }

  /**
   * Handles login.
   */
  public void login(HttpSession session, User user) {
    session.setAttribute(SessionKeys.CURRENT_USER_ID, user.getUserId());
  }

  /**
   * Handles logout.
   */
  public void logout(HttpSession session) {
    session.invalidate();
  }
}
