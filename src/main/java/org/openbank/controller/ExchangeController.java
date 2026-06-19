package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.ExchangeCalculationRequest;
import org.openbank.dto.TransferBetweenAccountsRequest;
import org.openbank.model.Currency;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.ExchangeCurrencyService;
import org.openbank.service.MessageService;
import org.openbank.service.TransactionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class ExchangeController {

  private final ExchangeCurrencyService exchangeCurrencyService;
  private final CurrentUserService currentUserService;
  private final TransactionService transactionService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public ExchangeController(ExchangeCurrencyService exchangeCurrencyService, CurrentUserService currentUserService, TransactionService transactionService, BankViewService bankViewService, MessageService messageService) {
    this.exchangeCurrencyService = exchangeCurrencyService;
    this.currentUserService = currentUserService;
    this.transactionService = transactionService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/exchange")
  public String exchange(HttpSession session, Model model) {
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange/calculate")
  public String calculate(@Valid @ModelAttribute ExchangeCalculationRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("exchangeError", firstError(bindingResult));
      addExchangeModel(session, model);
      return "exchange/index";
    }

    try {
      var result = exchangeCurrencyService.calculate(request.getFromCurrencyId(), request.getToCurrencyId(), request.getAmount());
      model.addAttribute("calculationResult", bankViewService.formatMoney(result));
    } catch (IllegalArgumentException e) {
      model.addAttribute("exchangeError", e.getMessage());
    }
    addExchangeModel(session, model);
    return "exchange/index";
  }

  @PostMapping("/exchange")
  public String exchangeMoney(@Valid @ModelAttribute TransferBetweenAccountsRequest request, BindingResult bindingResult, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("exchangeError", firstError(bindingResult));
      return "redirect:/exchange";
    }

    try {
      transactionService.makeTransactionExchangeCurrencies(request.getSenderAccountId(), request.getReceiverAccountId(), request.getAmount());
      redirectAttributes.addFlashAttribute("exchangeSuccess", messageService.get("transfers.exchange.success"));
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

  private String firstError(BindingResult bindingResult) {
    FieldError error = bindingResult.getFieldErrors().getFirst();
    return error.getDefaultMessage();
  }

  private Optional<Long> currentUserId(HttpSession session) {
    return currentUserService.getCurrentUser(session).map(User::getUserId);
  }
}
