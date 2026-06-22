package org.openbank.service.impl;

import org.openbank.service.StaffSessionService;
import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.openbank.model.User;
import org.springframework.stereotype.Service;

@Service
public class StaffSessionServiceImpl implements StaffSessionService {

  public boolean isAdminLoggedIn(HttpSession session) {
    return session.getAttribute(SessionKeys.CURRENT_ADMIN_ID) instanceof Long;
  }

  public void loginAdmin(HttpSession session, User admin) {
    session.setAttribute(SessionKeys.CURRENT_ADMIN_ID, admin.getUserId());
  }

  public void logoutAdmin(HttpSession session) {
    session.removeAttribute(SessionKeys.CURRENT_ADMIN_ID);
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
