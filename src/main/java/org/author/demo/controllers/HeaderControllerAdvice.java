package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.StaffSessionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class HeaderControllerAdvice {

  private final CurrentUserService currentUserService;
  private final StaffSessionService staffSessionService;

  public HeaderControllerAdvice(CurrentUserService currentUserService, StaffSessionService staffSessionService) {
    this.currentUserService = currentUserService;
    this.staffSessionService = staffSessionService;
  }

  @ModelAttribute("loggedIn")
  public boolean loggedIn(HttpSession session) {
    return currentUserService.isLoggedIn(session);
  }

  @ModelAttribute("managerLoggedIn")
  public boolean managerLoggedIn(HttpSession session) {
    return staffSessionService.isManagerLoggedIn(session);
  }

  @ModelAttribute("adminLoggedIn")
  public boolean adminLoggedIn(HttpSession session) {
    return staffSessionService.isAdminLoggedIn(session);
  }
}
