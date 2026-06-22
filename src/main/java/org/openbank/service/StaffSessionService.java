package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.model.User;

/**
 * Defines admin and manager session operations.
 */
public interface StaffSessionService {

  /** Checks whether an admin login marker is present. */
  boolean isAdminLoggedIn(HttpSession session);

  /** Stores a successful admin login in the session. */
  void loginAdmin(HttpSession session, User admin);

  /** Removes admin login state from the session. */
  void logoutAdmin(HttpSession session);

  /** Checks whether a manager login marker is present. */
  boolean isManagerLoggedIn(HttpSession session);

  /** Stores a successful manager login in the session. */
  void loginManager(HttpSession session, User manager);

  /** Removes manager login state from the session. */
  void logoutManager(HttpSession session);
}
