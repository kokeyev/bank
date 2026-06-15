package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import org.openbank.model.Currency;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.ExchangeCurrencyService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
public class ExchangeController {

  private final ExchangeCurrencyService exchangeCurrencyService;
  private final CurrentUserService currentUserService;
  private final TransactionService transactionService;
  private final BankViewService bankViewService;

  public ExchangeController(ExchangeCurrencyService exchangeCurrencyService, CurrentUserService currentUserService, TransactionService transactionService, BankViewService bankViewService) {
    this.exchangeCurrencyService = exchangeCurrencyService;
    this.currentUserService = currentUserService;
    this.transactionService = transactionService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/exchange")
  public String exchange(HttpSession session, Model model) {
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange/calculate")
  public String calculate(@RequestParam("fromCurrencyId") Long fromCurrencyId, @RequestParam("toCurrencyId") Long toCurrencyId, @RequestParam("amount") BigDecimal amount, HttpSession session, Model model) {
    try {
      BigDecimal result = exchangeCurrencyService.calculate(fromCurrencyId, toCurrencyId, amount);
      model.addAttribute("calculationResult", bankViewService.formatMoney(result));
    } catch (IllegalArgumentException e) {
      model.addAttribute("exchangeError", e.getMessage());
    }
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange")
  public String exchangeMoney(@RequestParam("senderAccountId") Long senderAccountId, @RequestParam("receiverAccountId") Long receiverAccountId, @RequestParam("amount") BigDecimal amount, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionExchangeCurrencies(senderAccountId, receiverAccountId, amount);
      redirectAttributes.addFlashAttribute("exchangeSuccess", "Обмен выполнен.");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("exchangeError", e.getMessage());
    }

    return "redirect:/exchange";
  }

  private void addExchangeModel(HttpSession session, Model model) {
    List<Currency> currencies = exchangeCurrencyService.getAllCurrencies();
    model.addAttribute("currencies", currencies);
    model.addAttribute("accountOptions", currentUserId(session).map(bankViewService::getTransferAccountOptions).orElse(List.of()));
    model.addAttribute("missingCurrencies", currentUserId(session).map(userId -> bankViewService.getMissingCurrencyNames(userId, currencies)).orElse(List.of()));
  }

  private Optional<Long> currentUserId(HttpSession session) {
    return currentUserService.getCurrentUser(session).map(User::getUserId);
  }
}
