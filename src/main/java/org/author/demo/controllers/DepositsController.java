package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.DepositTypeOption;
import org.author.demo.dto.OpenDepositRequest;
import org.author.demo.dto.TransferAccountOption;
import org.author.demo.model.Account;
import org.author.demo.model.DepositType;
import org.author.demo.model.User;
import org.author.demo.services.AccountService;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class DepositsController {

  private static final String KOPILKA = "Копилка";
  private static final String STRATEGY = "Стратегия";
  private static final String CAPITAL = "Капитал";

  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final DepositService depositService;

  public DepositsController(CurrentUserService currentUserService, AccountService accountService, DepositService depositService) {
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.depositService = depositService;
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
    } catch (RuntimeException e) {
      addDepositFormModel(session, model, productName, request);
      model.addAttribute("depositError", e.getMessage());
    }

    return template;
  }

  private void addDepositFormModel(HttpSession session, Model model, String productName, OpenDepositRequest request) {
    model.addAttribute("openDepositRequest", request);
    model.addAttribute("accountOptions", getAccountOptions(session));
    model.addAttribute("depositTypeOptions", getDepositTypeOptions(productName));
  }

  private List<TransferAccountOption> getAccountOptions(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return List.of();
    }

    return accountService.getAccountsByUserId(currentUser.get().getUserId())
        .stream()
        .map(this::toAccountOption)
        .toList();
  }

  private TransferAccountOption toAccountOption(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());
    String label = account.getName() + " - " + formatMoney(account.getBalance()) + " " + currency;

    return new TransferAccountOption(account.getAccountId(), label);
  }

  private List<DepositTypeOption> getDepositTypeOptions(String productName) {
    return depositService.getDepositTypesByProduct(productName)
        .stream()
        .map(this::toDepositTypeOption)
        .toList();
  }

  private DepositTypeOption toDepositTypeOption(DepositType depositType) {
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());
    String label = depositType.getDuration()
        + " мес. / "
        + currency
        + " / "
        + depositType.getRate()
        + "% / минимум "
        + formatMoney(depositType.getMinimumAmount());

    return new DepositTypeOption(depositType.getDepositTypeId(), label);
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');

    DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
    return format.format(amount);
  }
}
