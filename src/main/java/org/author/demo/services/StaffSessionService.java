package org.author.demo.services;

import jakarta.servlet.http.HttpSession;
import org.author.demo.controllers.SessionKeys;
import org.author.demo.model.User;
import org.springframework.stereotype.Service;

@Service
public class StaffSessionService {

  public boolean isAdminLoggedIn(HttpSession session) {
    return Boolean.TRUE.equals(session.getAttribute(SessionKeys.ADMIN_LOGGED_IN));
  }

  public boolean loginAdmin(HttpSession session, String login, String password) {
    if ("admin".equals(login) && "12345".equals(password)) {
      session.setAttribute(SessionKeys.ADMIN_LOGGED_IN, true);
      return true;
    }

    return false;
  }

  public void logoutAdmin(HttpSession session) {
    session.removeAttribute(SessionKeys.ADMIN_LOGGED_IN);
  }

  public boolean isManagerLoggedIn(HttpSession session) {
    return session.getAttribute(SessionKeys.CURRENT_MANAGER_ID) instanceof Long;
  }

  public void loginManager(HttpSession session, User manager) {
    session.setAttribute(SessionKeys.CURRENT_MANAGER_ID, manager.getUserId());
  }

  public void logoutManager(HttpSession session) {
    session.removeAttribute(SessionKeys.CURRENT_MANAGER_ID);
  }
}
