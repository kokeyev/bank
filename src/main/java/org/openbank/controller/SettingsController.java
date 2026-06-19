package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.PasswordChangeRequest;
import org.openbank.dto.UpdateContactRequest;
import org.openbank.model.User;
import org.openbank.service.CurrentUserService;
import org.openbank.exception.ContactUpdateException;
import org.openbank.service.MessageService;
import org.openbank.service.UserService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class SettingsController {

  private final CurrentUserService currentUserService;
  private final UserService userService;
  private final MessageService messageService;

  public SettingsController(CurrentUserService currentUserService, UserService userService, MessageService messageService) {
    this.currentUserService = currentUserService;
    this.userService = userService;
    this.messageService = messageService;
  }

  @GetMapping("/settings")
  public String settings(HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isPresent()) {
      User user = currentUser.get();
      model.addAttribute("phonePlaceholder", user.getPhoneNumber());
      model.addAttribute("emailPlaceholder", user.getEmailAddress());
    }
    addSelectedLanguage(model);

    return "settings/index";
  }

  @PostMapping("/settings/contact")
  public String updateContactDetails(@Valid @ModelAttribute UpdateContactRequest updateContactRequest, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addContactPlaceholders(model, currentUser.get());
      List<String> errorMessages = new ArrayList<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        errorMessages.add(error.getDefaultMessage());
      }
      model.addAttribute("contactErrors", errorMessages);
      addSelectedLanguage(model);
      return "settings/index";
    }

    try {
      User updatedUser = userService.updateContactDetails(currentUser.get(), updateContactRequest);
      addContactPlaceholders(model, updatedUser);
      model.addAttribute("contactSuccess", messageService.get("settings.contact.success"));
    } catch (ContactUpdateException e) {
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("contactErrors", e.getErrors());
    }

    addSelectedLanguage(model);
    return "settings/index";
  }

  @PostMapping("/settings/language")
  public String changeLanguage(@RequestParam("language") String language) {
    return "redirect:/settings";
  }

  @PostMapping("/settings/password")
  public String changePassword(@Valid @ModelAttribute PasswordChangeRequest passwordChangeRequest, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addContactPlaceholders(model, currentUser.get());
      List<String> errorMessages = new ArrayList<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        errorMessages.add(error.getDefaultMessage());
      }
      model.addAttribute("passwordErrors", errorMessages);
      addSelectedLanguage(model);
      return "settings/index";
    }

    try {
      userService.changePassword(currentUser.get(), passwordChangeRequest);
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("passwordSuccess", messageService.get("settings.password.success"));
    } catch (ContactUpdateException e) {
      addContactPlaceholders(model, currentUser.get());
      model.addAttribute("passwordErrors", e.getErrors());
    }

    addSelectedLanguage(model);
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

  private void addSelectedLanguage(Model model) {
    model.addAttribute("selectedLanguage", LocaleContextHolder.getLocale().getLanguage());
  }
}
