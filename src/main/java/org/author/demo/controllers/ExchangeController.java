package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.TransferAccountOption;
import org.author.demo.model.Account;
import org.author.demo.model.Currency;
import org.author.demo.model.User;
import org.author.demo.model.status.AccountStatus;
import org.author.demo.services.AccountService;
import org.author.demo.services.CurrentUserService;
import org.author.demo.services.ExchangeCurrencyService;
import org.author.demo.services.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class ExchangeController {

  private final ExchangeCurrencyService exchangeCurrencyService;
  private final CurrentUserService currentUserService;
  private final AccountService accountService;
  private final TransactionService transactionService;

  public ExchangeController(ExchangeCurrencyService exchangeCurrencyService, CurrentUserService currentUserService, AccountService accountService, TransactionService transactionService) {
    this.exchangeCurrencyService = exchangeCurrencyService;
    this.currentUserService = currentUserService;
    this.accountService = accountService;
    this.transactionService = transactionService;
  }

  @GetMapping("/exchange")
  public String exchange(HttpSession session, Model model) {
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange/calculate")
  public String calculate(
      @RequestParam("fromCurrencyId") Long fromCurrencyId,
      @RequestParam("toCurrencyId") Long toCurrencyId,
      @RequestParam("amount") BigDecimal amount,
      HttpSession session,
      Model model
  ) {
    try {
      BigDecimal result = exchangeCurrencyService.calculate(fromCurrencyId, toCurrencyId, amount);
      model.addAttribute("calculationResult", formatMoney(result));
    } catch (RuntimeException e) {
      model.addAttribute("exchangeError", e.getMessage());
    }
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange")
  public String exchangeMoney(
      @RequestParam("senderAccountId") Long senderAccountId,
      @RequestParam("receiverAccountId") Long receiverAccountId,
      @RequestParam("amount") BigDecimal amount,
      HttpSession session,
      RedirectAttributes redirectAttributes
  ) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      transactionService.makeTransactionExchangeCurrencies(senderAccountId, receiverAccountId, amount);
      redirectAttributes.addFlashAttribute("exchangeSuccess", "Обмен выполнен.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("exchangeError", e.getMessage());
    }

    return "redirect:/exchange";
  }

  private void addExchangeModel(HttpSession session, Model model) {
    List<Currency> currencies = exchangeCurrencyService.getAllCurrencies();
    model.addAttribute("currencies", currencies);
    model.addAttribute("accountOptions", getAccountOptions(session));
    model.addAttribute("missingCurrencies", getMissingCurrencies(session, currencies));
  }

  private List<TransferAccountOption> getAccountOptions(HttpSession session) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return List.of();
    }

    return accountService.getAccountsByUserId(currentUser.get().getUserId())
        .stream()
        .filter(account -> AccountStatus.ACTIVE.name().equals(account.getStatus()))
        .map(this::toAccountOption)
        .toList();
  }

  private List<String> getMissingCurrencies(HttpSession session, List<Currency> currencies) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return List.of();
    }

    List<Long> userCurrencyIds = accountService.getAccountsByUserId(currentUser.get().getUserId())
        .stream()
        .filter(account -> AccountStatus.ACTIVE.name().equals(account.getStatus()))
        .map(Account::getCurrencyId)
        .distinct()
        .toList();

    return currencies.stream()
        .filter(currency -> !userCurrencyIds.contains(currency.getCurrencyId()))
        .map(Currency::getName)
        .toList();
  }

  private TransferAccountOption toAccountOption(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());
    String label = account.getName() + " - " + formatMoney(account.getBalance()) + " " + currency;
    return new TransferAccountOption(account.getAccountId(), label);
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');
    return new DecimalFormat("#,##0.00", symbols).format(amount);
  }
}
