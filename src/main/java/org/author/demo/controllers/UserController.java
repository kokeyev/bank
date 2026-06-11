package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.CreateUserRequest;
import org.author.demo.model.User;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.UserRegistrationException;
import org.author.demo.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class UserController {

  private final UserService userService;
  private final CurrentUserService currentUserService;

  public UserController(UserService userService, CurrentUserService currentUserService) {
    this.userService = userService;
    this.currentUserService = currentUserService;
  }

  @GetMapping("/login")
  public String login(@RequestParam(value = "loginRequired", required = false) Boolean loginRequired, Model model) {
    if (Boolean.TRUE.equals(loginRequired)) {
      model.addAttribute("loginError", "Войдите в аккаунт, чтобы выполнить это действие.");
    }
    return "bank/login";
  }

  @PostMapping("/login")
  public String createLogin(@RequestParam("login") String login, @RequestParam("password") String password, HttpSession session, Model model) {
    Optional<User> user = userService.authenticate(login, password);

    if (user.isEmpty()) {
      model.addAttribute("loginError", "Неверный логин или пароль.");
      model.addAttribute("login", login);
      return "bank/login";
    }

    currentUserService.login(session, user.get());
    return "redirect:/accounts";
  }

  @PostMapping("/logout")
  public String logout(HttpSession session) {
    currentUserService.logout(session);
    return "redirect:/login";
  }

  @GetMapping("/register")
  public String register(Model model) {
    model.addAttribute("createUserRequest", new CreateUserRequest());
    return "bank/register";
  }

  @PostMapping("/register")
  public String createUser(@ModelAttribute CreateUserRequest createUserRequest, Model model) {
    try {
      userService.createUser(createUserRequest);
      return "redirect:/login";
    } catch (UserRegistrationException e) {
      model.addAttribute("createUserRequest", createUserRequest);
      model.addAttribute("errors", e.getErrors());
      return "bank/register";
    }
  }
}
