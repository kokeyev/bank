package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

/**
 * Provides staff session service operations.
 */
@Service
public class StaffSessionService {

  /**
   * Handles is admin logged in.
   */
  public boolean isAdminLoggedIn(HttpSession session) {
    return Boolean.TRUE.equals(session.getAttribute(SessionKeys.ADMIN_LOGGED_IN));
  }

  /**
   * Handles login admin.
   */
  public boolean loginAdmin(HttpSession session, String login, String password) {
    if ("admin".equals(login) && "12345".equals(password)) {
      session.setAttribute(SessionKeys.ADMIN_LOGGED_IN, true);
      return true;
    }

    return false;
  }

  /**
   * Handles logout admin.
   */
  public void logoutAdmin(HttpSession session) {
    session.removeAttribute(SessionKeys.ADMIN_LOGGED_IN);
  }

  /**
   * Handles is manager logged in.
   */
  public boolean isManagerLoggedIn(HttpSession session) {
    return session.getAttribute(SessionKeys.CURRENT_MANAGER_ID) instanceof Long;
  }

  /**
   * Handles login manager.
   */
  public void loginManager(HttpSession session, User manager) {
    session.setAttribute(SessionKeys.CURRENT_MANAGER_ID, manager.getUserId());
  }

  /**
   * Handles logout manager.
   */
  public void logoutManager(HttpSession session) {
    session.removeAttribute(SessionKeys.CURRENT_MANAGER_ID);
  }
}
