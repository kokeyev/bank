package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import org.openbank.dto.OpenDepositRequest;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Optional;

@Controller
public class DepositsController {

  private static final String KOPILKA = "Копилка";
  private static final String STRATEGY = "Стратегия";
  private static final String CAPITAL = "Капитал";

  private final CurrentUserService currentUserService;
  private final DepositService depositService;
  private final BankViewService bankViewService;

  public DepositsController(CurrentUserService currentUserService, DepositService depositService, BankViewService bankViewService) {
    this.currentUserService = currentUserService;
    this.depositService = depositService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/deposits")
  public String deposits() {
    return "deposits/index";
  }

  @GetMapping("/deposits/kopilka")
  public String kopilkaDeposit(HttpSession session, Model model) {
    addDepositFormModel(session, model, KOPILKA, new OpenDepositRequest());

    return "deposits/kopilka";
  }

  @PostMapping("/deposits/kopilka")
  public String createKopilkaDeposit(@ModelAttribute OpenDepositRequest request, HttpSession session, Model model) {
    return openDeposit(KOPILKA, "deposits/kopilka", request, session, model);
  }

  @GetMapping("/deposits/strategy")
  public String strategyDeposit(HttpSession session, Model model) {
    addDepositFormModel(session, model, STRATEGY, new OpenDepositRequest());

    return "deposits/strategy";
  }

  @PostMapping("/deposits/strategy")
  public String createStrategyDeposit(@ModelAttribute OpenDepositRequest request, HttpSession session, Model model) {
    return openDeposit(STRATEGY, "deposits/strategy", request, session, model);
  }

  @GetMapping("/deposits/capital")
  public String capitalDeposit(HttpSession session, Model model) {
    OpenDepositRequest request = new OpenDepositRequest();
    request.setReinvestInterest(true);
    addDepositFormModel(session, model, CAPITAL, request);

    return "deposits/capital";
  }

  @PostMapping("/deposits/capital")
  public String createCapitalDeposit(@ModelAttribute OpenDepositRequest request, HttpSession session, Model model) {
    return openDeposit(CAPITAL, "deposits/capital", request, session, model);
  }

  private String openDeposit(String productName, String template, OpenDepositRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      depositService.openDeposit(currentUser.get().getUserId(), request);
      addDepositFormModel(session, model, productName, new OpenDepositRequest());
      model.addAttribute("depositSuccess", "Заявка на депозит отправлена менеджеру.");
    } catch (IllegalArgumentException e) {
      addDepositFormModel(session, model, productName, request);
      model.addAttribute("depositError", e.getMessage());
    }

    return template;
  }

  private void addDepositFormModel(HttpSession session, Model model, String productName, OpenDepositRequest request) {
    model.addAttribute("openDepositRequest", request);
    model.addAttribute("accountOptions", currentUserId(session).map(bankViewService::getAllAccountOptions).orElse(List.of()));
    model.addAttribute("depositTypeOptions", bankViewService.getDepositTypeOptions(productName));
  }

  private Optional<Long> currentUserId(HttpSession session) {
    return currentUserService.getCurrentUser(session).map(User::getUserId);
  }
}
