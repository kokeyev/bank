package org.openbank.service.impl;

import org.openbank.service.CurrentUserService;
import org.openbank.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserServiceImpl implements CurrentUserService {

  private final UserService userService;
  public CurrentUserServiceImpl(UserService userService) {
    this.userService = userService;
  }

  public Optional<Long> getCurrentUserId(HttpSession session) {
    Object currentUserId = session.getAttribute(SessionKeys.CURRENT_USER_ID);

    if (currentUserId instanceof Long userId) {
      return Optional.of(userId);
    }

    return Optional.empty();
  }

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

  public boolean isLoggedIn(HttpSession session) {
    return getCurrentUser(session).isPresent();
  }

  public void login(HttpSession session, User user) {
    session.setAttribute(SessionKeys.CURRENT_USER_ID, user.getUserId());
  }

  public void logout(HttpSession session) {
    session.invalidate();
  }
}
