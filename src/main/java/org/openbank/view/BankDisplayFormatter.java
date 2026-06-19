package org.openbank.view;

import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.DepositStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class BankDisplayFormatter {

  private static final DateTimeFormatter EXPIRY_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/yy");
  private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final MessageService messageService;

  public BankDisplayFormatter(MessageService messageService) {
    this.messageService = messageService;
  }

  public String money(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');
    return new DecimalFormat("#,##0.00", symbols).format(amount);
  }

  public String decimalValue(BigDecimal amount) {
    return amount == null ? "0.00" : amount.stripTrailingZeros().toPlainString();
  }

  public String cardNumber(String cardNumber) {
    if (cardNumber == null || cardNumber.isBlank()) {
      return "";
    }

    return cardNumber.replaceAll("(.{4})(?!$)", "$1 ");
  }

  public String expiryDate(LocalDate expiryDate) {
    return expiryDate == null ? "" : expiryDate.format(EXPIRY_DATE_FORMATTER);
  }

  public String displayDate(LocalDate date) {
    return date == null ? "" : date.format(DISPLAY_DATE_FORMATTER);
  }

  public String displayDateOrDash(LocalDate date) {
    return date == null ? "-" : date.format(DISPLAY_DATE_FORMATTER);
  }

  public String rate(BigDecimal rate) {
    if (rate == null) {
      return "0%";
    }

    return rate.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
  }

  public String duration(Integer duration) {
    if (duration == null) {
      return "";
    }

    if (duration >= 12 && duration % 12 == 0) {
      int years = duration / 12;
      return messageService.get(years == 1 ? "format.year.one" : "format.year.many", years);
    }

    return messageService.get("format.months", duration);
  }

  public String yesNo(Boolean value) {
    return messageService.get(Boolean.TRUE.equals(value) ? "common.yes" : "common.no");
  }

  public String accountStatus(String status) {
    if (AccountStatus.PENDING.name().equals(status)) {
      return messageService.get("status.pending");
    }
    if (AccountStatus.ACTIVE.name().equals(status)) {
      return messageService.get("status.active");
    }
    if (AccountStatus.DEACTIVATED.name().equals(status)) {
      return messageService.get("status.deactivated");
    }
    if (AccountStatus.EXPIRED.name().equals(status)) {
      return messageService.get("status.expired");
    }
    if (AccountStatus.DELETED.name().equals(status)) {
      return messageService.get("status.deleted");
    }
    if (AccountStatus.REJECTED.name().equals(status)) {
      return messageService.get("status.rejected");
    }
    return status == null ? "" : status;
  }

  public String depositStatus(String status) {
    if (DepositStatus.PENDING.name().equals(status)) {
      return messageService.get("status.pending");
    }
    if (DepositStatus.ACTIVE.name().equals(status)) {
      return messageService.get("status.active");
    }
    if (DepositStatus.EXPIRED.name().equals(status)) {
      return messageService.get("status.expired");
    }
    if (DepositStatus.REJECTED.name().equals(status)) {
      return messageService.get("status.rejected");
    }
    if (DepositStatus.CLOSED.name().equals(status)) {
      return messageService.get("status.closed");
    }
    return status == null ? "" : status;
  }

  public String loanStatus(String status) {
    if (LoanStatus.PENDING.name().equals(status)) {
      return messageService.get("status.pending");
    }
    if (LoanStatus.OFFERED.name().equals(status)) {
      return messageService.get("status.offered");
    }
    if (LoanStatus.ACTIVE.name().equals(status)) {
      return messageService.get("status.active");
    }
    if (LoanStatus.REFUSED.name().equals(status)) {
      return messageService.get("status.refusedByClient");
    }
    if (LoanStatus.REJECTED.name().equals(status)) {
      return messageService.get("status.rejectedByManager");
    }
    if (LoanStatus.CLOSED.name().equals(status)) {
      return messageService.get("status.closed");
    }
    return status == null ? "" : status;
  }
}
