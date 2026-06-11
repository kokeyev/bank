package org.author.demo.controllers;

import org.author.demo.dto.DepositView;
import org.author.demo.model.Deposit;
import org.author.demo.model.DepositType;
import org.author.demo.model.status.DepositStatus;
import org.author.demo.services.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class ManagerDepositsController {

  private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final DepositService depositService;

  public ManagerDepositsController(DepositService depositService) {
    this.depositService = depositService;
  }

  @GetMapping("/manager/deposits")
  public String deposits(Model model) {
    model.addAttribute("pendingDeposits", getDepositViews());
    return "manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/approve")
  public String approve(@PathVariable Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.approveDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", "Депозит одобрен.");
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", "Не удалось одобрить депозит.");
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/reject")
  public String reject(@PathVariable Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.rejectDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", "Депозит отклонен.");
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", "Не удалось отклонить депозит.");
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/process-expiration")
  public String processExpiration(RedirectAttributes redirectAttributes) {
    int count = depositService.processExpiredDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", "Обработано депозитов: " + count + ".");
    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/accrue-interest")
  public String accrueInterest(RedirectAttributes redirectAttributes) {
    int count = depositService.accrueInterestForActiveDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", "Начислено вознаграждение по депозитам: " + count + ".");
    return "redirect:/manager/deposits";
  }

  private List<DepositView> getDepositViews() {
    return depositService.getPendingDeposits()
        .stream()
        .map(this::toDepositView)
        .toList();
  }

  private DepositView toDepositView(Deposit deposit) {
    DepositType depositType = depositService.getDepositTypeById(deposit.getDepositTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип депозита не найден"));
    String currency = depositService.getCurrencyNameById(depositType.getCurrencyId());

    return new DepositView(
        deposit.getDepositId(),
        depositType.getName(),
        formatMoney(deposit.getCurrentAmount()),
        formatDecimalValue(deposit.getCurrentAmount()),
        currency,
        formatRate(depositType.getRate()),
        depositType.getDuration() + " мес.",
        formatDisplayDate(deposit.getStartDate()),
        translateStatus(deposit.getStatus()),
        formatBoolean(deposit.getAutoRenewal()),
        formatBoolean(deposit.getReinvestInterest()),
        DepositStatus.ACTIVE.name().equals(deposit.getStatus()),
        Boolean.TRUE.equals(depositType.getWithdrawal())
    );
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');
    return new DecimalFormat("#,##0.00", symbols).format(amount);
  }

  private String formatDecimalValue(BigDecimal amount) {
    return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
  }

  private String formatRate(BigDecimal rate) {
    if (rate == null) {
      return "0%";
    }

    return rate.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
  }

  private String formatDisplayDate(LocalDate date) {
    return date == null ? "" : date.format(DISPLAY_DATE_FORMATTER);
  }

  private String formatBoolean(Boolean value) {
    return Boolean.TRUE.equals(value) ? "Да" : "Нет";
  }

  private String translateStatus(String status) {
    if (DepositStatus.PENDING.name().equals(status)) {
      return "На рассмотрении";
    }
    return status == null ? "" : status;
  }
}
