package org.author.demo.services;

import jakarta.servlet.http.HttpSession;
import org.author.demo.controllers.SessionKeys;
import org.author.demo.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

  private final UserService userService;

  public CurrentUserService(UserService userService) {
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
