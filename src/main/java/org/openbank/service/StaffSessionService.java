package org.openbank.service;

import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

/**
 * Stores staff login state in the HTTP session for admin and manager areas.
 *
 * <p>Authentication is performed by {@link UserService}; this service only records the authenticated
 * staff user's database id in the session.</p>
 */
@Service
public class StaffSessionService {

  /**
   * Checks whether the current session has an authenticated admin id.
   *
   * @param session current HTTP session
   * @return {@code true} when an admin login marker is present
   */
  public boolean isAdminLoggedIn(HttpSession session) {
    return session.getAttribute(SessionKeys.CURRENT_ADMIN_ID) instanceof Long;
  }

  /**
   * Stores a successful admin login in the session.
   *
   * @param session current HTTP session
   * @param admin authenticated admin user loaded from the database
   */
  public void loginAdmin(HttpSession session, User admin) {
    session.setAttribute(SessionKeys.CURRENT_ADMIN_ID, admin.getUserId());
  }

  /**
   * Removes admin login state without affecting client or manager markers.
   *
   * @param session current HTTP session
   */
  public void logoutAdmin(HttpSession session) {
    session.removeAttribute(SessionKeys.CURRENT_ADMIN_ID);
  }

  /**
   * Checks whether the current session has an authenticated manager id.
   *
   * @param session current HTTP session
   * @return {@code true} when a manager login marker is present
   */
  public boolean isManagerLoggedIn(HttpSession session) {
    return session.getAttribute(SessionKeys.CURRENT_MANAGER_ID) instanceof Long;
  }

  /**
   * Stores a successful manager login in the session.
   *
   * @param session current HTTP session
   * @param manager authenticated manager user
   */
  public void loginManager(HttpSession session, User manager) {
    session.setAttribute(SessionKeys.CURRENT_MANAGER_ID, manager.getUserId());
  }

  /**
   * Removes manager login state without affecting client or admin markers.
   *
   * @param session current HTTP session
   */
  public void logoutManager(HttpSession session) {
    session.removeAttribute(SessionKeys.CURRENT_MANAGER_ID);
  }
}
