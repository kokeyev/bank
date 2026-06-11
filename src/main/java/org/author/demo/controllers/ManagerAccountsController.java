package org.author.demo.controllers;

import org.author.demo.dto.AccountView;
import org.author.demo.model.Account;
import org.author.demo.model.status.AccountStatus;
import org.author.demo.services.AccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class ManagerAccountsController {

  private static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");

  private final AccountService accountService;

  public ManagerAccountsController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping("/manager/accounts")
  public String pendingAccounts(Model model) {
    model.addAttribute("pendingAccounts", getPendingAccountViews());
    return "manager/accounts";
  }

  @PostMapping("/manager/accounts/{accountId}/approve")
  public String approveAccount(@PathVariable Long accountId, RedirectAttributes redirectAttributes) {
    try {
      accountService.approveAccount(accountId);
      redirectAttributes.addFlashAttribute("managerAccountSuccess", "Заявка на счет одобрена.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("managerAccountError", e.getMessage());
    }

    return "redirect:/manager/accounts";
  }

  @PostMapping("/manager/accounts/{accountId}/reject")
  public String rejectAccount(@PathVariable Long accountId, RedirectAttributes redirectAttributes) {
    try {
      accountService.rejectAccount(accountId);
      redirectAttributes.addFlashAttribute("managerAccountSuccess", "Заявка на счет отклонена.");
    } catch (RuntimeException e) {
      redirectAttributes.addFlashAttribute("managerAccountError", e.getMessage());
    }

    return "redirect:/manager/accounts";
  }

  private List<AccountView> getPendingAccountViews() {
    return accountService.getPendingAccounts()
        .stream()
        .map(this::toAccountView)
        .toList();
  }

  private AccountView toAccountView(Account account) {
    String currency = accountService.getCurrencyNameById(account.getCurrencyId());

    return new AccountView(
        account.getAccountId(),
        account.getName(),
        formatMoney(account.getBalance()),
        currency,
        formatCardNumber(account.getCardNumber()),
        formatExpiryDate(account.getExpiryDate()),
        account.getCvv(),
        formatMoney(account.getTransactionLimit()),
        formatDecimalValue(account.getTransactionLimit()),
        account.getStatus(),
        "На рассмотрении",
        Boolean.TRUE.equals(account.getMain()),
        AccountStatus.ACTIVE.name().equals(account.getStatus())
    );
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

  private String formatDecimalValue(BigDecimal amount) {
    return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
  }

  private String formatCardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      return "";
    }

    return cardNumber.replaceAll("(.{4})(?!$)", "$1 ");
  }

  private String formatExpiryDate(LocalDate expiryDate) {
    return expiryDate == null ? "" : expiryDate.format(EXPIRY_DATE_FORMATTER);
  }
}
