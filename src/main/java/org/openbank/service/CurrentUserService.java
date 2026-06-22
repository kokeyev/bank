package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.model.User;

import java.util.Optional;

/**
 * Defines client session user operations.
 */
public interface CurrentUserService {

  /** Extracts the current client id from the session. */
  Optional<Long> getCurrentUserId(HttpSession session);

  /** Loads the current client and clears stale session ids. */
  Optional<User> getCurrentUser(HttpSession session);

  /** Checks whether a valid client is logged in. */
  boolean isLoggedIn(HttpSession session);

  /** Stores a successful client login in the session. */
  void login(HttpSession session, User user);

  /** Ends the client session. */
  void logout(HttpSession session);
}
