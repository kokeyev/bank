package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import org.openbank.dto.CreateUserRequest;
import org.openbank.model.User;
import org.openbank.service.StaffSessionService;
import org.openbank.exception.UserRegistrationException;
import org.openbank.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ManagerPortalController {

  private final UserService userService;
  private final StaffSessionService staffSessionService;

  public ManagerPortalController(UserService userService, StaffSessionService staffSessionService) {
    this.userService = userService;
    this.staffSessionService = staffSessionService;
  }

  @GetMapping("/manager")
  public String manager(HttpSession session, Model model) {
    if (staffSessionService.isManagerLoggedIn(session)) {
      return "redirect:/manager/accounts";
    }

    model.addAttribute("createUserRequest", new CreateUserRequest());
    return "manager/index";
  }

  @PostMapping("/manager/login")
  public String login(@RequestParam("login") String login, @RequestParam("password") String password, HttpSession session, Model model) {
    Optional<User> manager = userService.authenticateManager(login, password);

    if (manager.isEmpty()) {
      model.addAttribute("managerLoginError", "Неверный логин, пароль или менеджер еще не одобрен админом.");
      model.addAttribute("managerLogin", login);
      model.addAttribute("createUserRequest", new CreateUserRequest());
      return "manager/index";
    }

    staffSessionService.loginManager(session, manager.get());
    return "redirect:/manager/accounts";
  }

  @PostMapping("/manager/register")
  public String register(@ModelAttribute CreateUserRequest createUserRequest, Model model) {
    try {
      userService.createManager(createUserRequest);
      model.addAttribute("managerRegisterSuccess", "Заявка менеджера отправлена админу.");
      model.addAttribute("createUserRequest", new CreateUserRequest());
    } catch (UserRegistrationException e) {
      model.addAttribute("createUserRequest", createUserRequest);
      model.addAttribute("managerRegisterErrors", e.getErrors());
    }

    return "manager/index";
  }

  @PostMapping("/manager/logout")
  public String logout(HttpSession session) {
    staffSessionService.logoutManager(session);
    return "redirect:/manager";
  }
}
