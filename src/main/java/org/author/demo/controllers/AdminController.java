package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dao.depositType.DepositTypeDao;
import org.author.demo.dao.loanType.LoanTypeDao;
import org.author.demo.model.User;
import org.author.demo.services.BankSettingsService;
import org.author.demo.services.ExchangeCurrencyService;
import org.author.demo.services.StaffSessionService;
import org.author.demo.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class AdminController {

  private final StaffSessionService staffSessionService;
  private final UserService userService;
  private final ExchangeCurrencyService exchangeCurrencyService;
  private final DepositTypeDao depositTypeDao;
  private final LoanTypeDao loanTypeDao;
  private final BankSettingsService bankSettingsService;

  public AdminController(StaffSessionService staffSessionService, UserService userService, ExchangeCurrencyService exchangeCurrencyService, DepositTypeDao depositTypeDao, LoanTypeDao loanTypeDao, BankSettingsService bankSettingsService) {
    this.staffSessionService = staffSessionService;
    this.userService = userService;
    this.exchangeCurrencyService = exchangeCurrencyService;
    this.depositTypeDao = depositTypeDao;
    this.loanTypeDao = loanTypeDao;
    this.bankSettingsService = bankSettingsService;
  }

  @GetMapping("/admin")
  public String admin(HttpSession session, Model model) {
    if (!staffSessionService.isAdminLoggedIn(session)) {
      return "admin/login";
    }

    addAdminModel(model);
    return "admin/index";
  }

  @PostMapping("/admin/login")
  public String login(@RequestParam("login") String login, @RequestParam("password") String password, HttpSession session, Model model) {
    if (!staffSessionService.loginAdmin(session, login, password)) {
      model.addAttribute("adminLoginError", "Неверный логин или пароль.");
      model.addAttribute("adminLogin", login);
      return "admin/login";
    }

    return "redirect:/admin";
  }

  @PostMapping("/admin/logout")
  public String logout(HttpSession session) {
    staffSessionService.logoutAdmin(session);
    return "redirect:/admin";
  }

  @PostMapping("/admin/managers/{managerId}/approve")
  public String approveManager(@PathVariable Long managerId, RedirectAttributes redirectAttributes) {
    try {
      if (userService.approveManager(managerId)) {
        redirectAttributes.addFlashAttribute("adminSuccess", "Менеджер одобрен.");
      } else {
        redirectAttributes.addFlashAttribute("adminError", "Не удалось одобрить менеджера.");
      }
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  @PostMapping("/admin/managers/{managerId}/reject")
  public String rejectManager(@PathVariable Long managerId, RedirectAttributes redirectAttributes) {
    try {
      if (userService.rejectManager(managerId)) {
        redirectAttributes.addFlashAttribute("adminSuccess", "Менеджер отклонен.");
      } else {
        redirectAttributes.addFlashAttribute("adminError", "Не удалось отклонить менеджера.");
      }
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/fee")
  public String updateFee(@RequestParam("transferFeePercent") BigDecimal transferFeePercent, RedirectAttributes redirectAttributes) {
    try {
      bankSettingsService.setTransferFeePercent(transferFeePercent);
      redirectAttributes.addFlashAttribute("adminSuccess", "Комиссия обновлена.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/currency-rate")
  public String updateCurrencyRate(@RequestParam("currencyId") Long currencyId, @RequestParam("rateToKzt") BigDecimal rateToKzt, RedirectAttributes redirectAttributes) {
    try {
      exchangeCurrencyService.updateRate(currencyId, rateToKzt);
      redirectAttributes.addFlashAttribute("adminSuccess", "Курс валюты обновлен.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/deposit-rate")
  public String updateDepositRate(@RequestParam("depositTypeId") Long depositTypeId, @RequestParam("rate") BigDecimal rate, RedirectAttributes redirectAttributes) {
    try {
      depositTypeDao.changeRateOfDepositType(depositTypeId, rate);
      redirectAttributes.addFlashAttribute("adminSuccess", "Ставка депозита обновлена.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  @PostMapping("/admin/settings/loan-rate")
  public String updateLoanRate(@RequestParam("loanTypeId") Long loanTypeId, @RequestParam("rate") BigDecimal rate, RedirectAttributes redirectAttributes) {
    try {
      loanTypeDao.changeRateOfLoanType(loanTypeId, rate);
      redirectAttributes.addFlashAttribute("adminSuccess", "Ставка кредита обновлена.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("adminError", e.getMessage());
    }
    return "redirect:/admin";
  }

  private void addAdminModel(Model model) {
    List<User> pendingManagers = userService.getPendingManagers();
    model.addAttribute("pendingManagers", pendingManagers);
    model.addAttribute("currencies", exchangeCurrencyService.getAllCurrencies());
    model.addAttribute("depositTypes", depositTypeDao.getAllDepositTypes());
    model.addAttribute("loanTypes", loanTypeDao.getAllLoanTypes());
    model.addAttribute("transferFeePercent", bankSettingsService.getTransferFeePercent());
  }
}
