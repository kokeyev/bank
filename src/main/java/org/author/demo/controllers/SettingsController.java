package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.PasswordChangeRequest;
import org.author.demo.dto.UpdateContactRequest;
import org.author.demo.model.User;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.ContactUpdateException;
import org.author.demo.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class SettingsController {

  private final CurrentUserService currentUserService;
  private final UserService userService;

  public SettingsController(CurrentUserService currentUserService, UserService userService) {
    this.currentUserService = currentUserService;
    this.userService = userService;
  }

  @GetMapping("/settings")
  public String settings(HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    currentUser.ifPresent(user -> {
      model.addAttribute("phonePlaceholder", user.getPhoneNumber());
      model.addAttribute("emailPlaceholder", user.getEmailAddress());
    });
    model.addAttribute("selectedLanguage", session.getAttribute("language") == null ? "ru" : session.getAttribute("language"));

    return "settings/index";
  }

  @PostMapping("/settings/contact")
  public String updateContactDetails(
      @ModelAttribute UpdateContactRequest updateContactRequest,
      HttpSession session,
      Model model
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      User updatedUser = userService.updateContactDetails(currentUser.get(), updateContactRequest);
      addContactPlaceholders(model, updatedUser);
      model.addAttribute("contactSuccess", "Контактные данные обновлены.");
    } catch (ContactUpdateException e) {
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("contactErrors", e.getErrors());
    }

    model.addAttribute("selectedLanguage", session.getAttribute("language") == null ? "ru" : session.getAttribute("language"));
    return "settings/index";
  }

  @PostMapping("/settings/language")
  public String changeLanguage(@RequestParam("language") String language, HttpSession session) {
    if (language == null || language.isBlank()) {
      language = "ru";
    }

    session.setAttribute("language", language);
    return "redirect:/settings";
  }

  @PostMapping("/settings/password")
  public String changePassword(
      @ModelAttribute PasswordChangeRequest passwordChangeRequest,
      HttpSession session,
      Model model
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      userService.changePassword(currentUser.get(), passwordChangeRequest);
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("passwordSuccess", "Пароль обновлен.");
    } catch (ContactUpdateException e) {
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("passwordErrors", e.getErrors());
    }

    model.addAttribute("selectedLanguage", session.getAttribute("language") == null ? "ru" : session.getAttribute("language"));
    return "settings/index";
  }

  @PostMapping("/settings/deactivate")
  public String deactivateAccount(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    userService.deactivateUser(currentUser.get().getUserId());
    session.invalidate();
    return "redirect:/?accountDeactivated=true";
  }

  private void addContactPlaceholders(Model model, User user) {
    model.addAttribute("phonePlaceholder", user.getPhoneNumber());
    model.addAttribute("emailPlaceholder", user.getEmailAddress());
  }
}
