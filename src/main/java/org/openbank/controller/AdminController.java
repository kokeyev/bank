package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dao.deposittype.DepositTypeDao;
import org.openbank.dao.loantype.LoanTypeDao;
import org.openbank.dto.AdminFeeUpdateRequest;
import org.openbank.dto.CurrencyRateUpdateRequest;
import org.openbank.dto.DepositRateUpdateRequest;
import org.openbank.dto.LoanRateUpdateRequest;
import org.openbank.dto.Page;
import org.openbank.model.User;
import org.openbank.service.BankSettingsService;
import org.openbank.service.ExchangeCurrencyService;
import org.openbank.service.MessageService;
import org.openbank.service.StaffSessionService;
import org.openbank.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class AdminController {

  private static final int MANAGER_PAGE_SIZE = 5;

  private final StaffSessionService staffSessionService;
  private final UserService userService;
  private final ExchangeCurrencyService exchangeCurrencyService;
  private final DepositTypeDao depositTypeDao;
  private final LoanTypeDao loanTypeDao;
  private final BankSettingsService bankSettingsService;
  private final MessageService messageService;

  public AdminController(StaffSessionService staffSessionService, UserService userService, ExchangeCurrencyService exchangeCurrencyService, DepositTypeDao depositTypeDao, LoanTypeDao loanTypeDao, BankSettingsService bankSettingsService, MessageService messageService) {
    this.staffSessionService = staffSessionService;
    this.userService = userService;
    this.exchangeCurrencyService = exchangeCurrencyService;
    this.depositTypeDao = depositTypeDao;
    this.loanTypeDao = loanTypeDao;
    this.bankSettingsService = bankSettingsService;
    this.messageService = messageService;
  }

  @GetMapping("/admin")
  public String admin(@RequestParam(value = "managersPage", defaultValue = "1") int managersPage, HttpSession session, Model model) {
    if (!staffSessionService.isAdminLoggedIn(session)) {
      return "admin/login";
    }

    addAdminModel(model, managersPage);

    return "admin/index";
  }

  @PostMapping("/admin/login")
  public String login(@RequestParam("login") String login, @RequestParam("password") String password, HttpSession session, Model model) {
    return userService.authenticateAdmin(login, password)
        .map(admin -> {
          staffSessionService.loginAdmin(session, admin);
          return "redirect:/admin";
        })
        .orElseGet(() -> {
      model.addAttribute("adminLoginError", messageService.get("auth.login.invalid"));
      model.addAttribute("adminLogin", login);

      return "admin/login";
        });
  }

  @PostMapping("/admin/logout")
  public String logout(HttpSession session) {
    staffSessionService.logoutAdmin(session);

    return "redirect:/admin";
  }

  @PostMapping("/admin/managers/{managerId}/approve")
  public String approveManager(@PathVariable("managerId") Long managerId, RedirectAttributes redirectAttributes) {
    try {
      if (userService.approveManager(managerId)) {
        redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.manager.approve.success"));
      } else {
        redirectAttributes.addFlashAttribute("adminError", messageService.get("admin.manager.approve.error"));
      }
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/managers/{managerId}/reject")
  public String rejectManager(@PathVariable("managerId") Long managerId, RedirectAttributes redirectAttributes) {
    try {
      if (userService.rejectManager(managerId)) {
        redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.manager.reject.success"));
      } else {
        redirectAttributes.addFlashAttribute("adminError", messageService.get("admin.manager.reject.error"));
      }
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/fee")
  public String updateFee(@Valid @ModelAttribute AdminFeeUpdateRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (hasValidationErrors(bindingResult, redirectAttributes)) {
      return "redirect:/admin";
    }

    try {
      bankSettingsService.setTransferFeePercent(request.getTransferFeePercent());
      redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.fee.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/currency-rate")
  public String updateCurrencyRate(@Valid @ModelAttribute CurrencyRateUpdateRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (hasValidationErrors(bindingResult, redirectAttributes)) {
      return "redirect:/admin";
    }

    try {
      exchangeCurrencyService.updateRate(request.getCurrencyId(), request.getRateToKzt());
      redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.currencyRate.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/deposit-rate")
  public String updateDepositRate(@Valid @ModelAttribute DepositRateUpdateRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (hasValidationErrors(bindingResult, redirectAttributes)) {
      return "redirect:/admin";
    }

    try {
      depositTypeDao.changeRateOfDepositType(request.getDepositTypeId(), request.getRate());
      redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.depositRate.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/loan-rate")
  public String updateLoanRate(@Valid @ModelAttribute LoanRateUpdateRequest request, BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (hasValidationErrors(bindingResult, redirectAttributes)) {
      return "redirect:/admin";
    }

    try {
      loanTypeDao.changeRateOfLoanType(request.getLoanTypeId(), request.getRate());
      redirectAttributes.addFlashAttribute("adminSuccess", messageService.get("admin.loanRate.success"));
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }

    return "redirect:/admin";
  }

  private void addAdminModel(Model model, int managersPage) {
    List<User> pendingManagers = userService.getPendingManagers();
    model.addAttribute("pendingManagersPage", new Page<>(pendingManagers, managersPage, MANAGER_PAGE_SIZE));
    model.addAttribute("currencies", exchangeCurrencyService.getAllCurrencies());
    model.addAttribute("depositTypes", depositTypeDao.getAllDepositTypes());
    model.addAttribute("loanTypes", loanTypeDao.getAllLoanTypes());
    model.addAttribute("transferFeePercent", bankSettingsService.getTransferFeePercent());
  }

  private boolean hasValidationErrors(BindingResult bindingResult, RedirectAttributes redirectAttributes) {
    if (!bindingResult.hasErrors()) {
      return false;
    }

    FieldError fieldError = bindingResult.getFieldErrors().getFirst();
    redirectAttributes.addFlashAttribute("adminError", fieldError.getDefaultMessage());
    return true;
  }
}
