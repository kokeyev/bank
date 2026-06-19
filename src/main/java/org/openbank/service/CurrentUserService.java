package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Reads and updates the client login state stored in the HTTP session.
 *
 * <p>Only the user id is stored in the session; user details are reloaded from the database when
 * needed so deleted or deactivated records are not trusted indefinitely.</p>
 */
@Service
public class CurrentUserService {

  private final UserService userService;
  public CurrentUserService(UserService userService) {
    this.userService = userService;
  }

  /**
   * Extracts the current client id from the session.
   *
   * @param session current HTTP session
   * @return user id when a client is logged in
   */
  public Optional<Long> getCurrentUserId(HttpSession session) {
    Object currentUserId = session.getAttribute(SessionKeys.CURRENT_USER_ID);

    if (currentUserId instanceof Long userId) {
      return Optional.of(userId);
    }

    return Optional.empty();
  }

  /**
   * Loads the current client from the database and clears stale session ids.
   *
   * @param session current HTTP session
   * @return current user when the session id still points to an existing user
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
   * Checks whether the session belongs to a valid logged-in client.
   *
   * @param session current HTTP session
   * @return {@code true} when a current user can be loaded
   */
  public boolean isLoggedIn(HttpSession session) {
    return getCurrentUser(session).isPresent();
  }

  /**
   * Stores a successful client login in the session.
   *
   * @param session current HTTP session
   * @param user authenticated client
   */
  public void login(HttpSession session, User user) {
    session.setAttribute(SessionKeys.CURRENT_USER_ID, user.getUserId());
  }

  /**
   * Ends the client session.
   *
   * @param session current HTTP session
   */
  public void logout(HttpSession session) {
    session.invalidate();
  }
}
