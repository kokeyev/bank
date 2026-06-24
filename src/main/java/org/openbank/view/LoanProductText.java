package org.openbank.view;

import org.openbank.service.MessageService;
import org.openbank.service.strategy.loan.AutoLoanStrategy;
import org.openbank.service.strategy.loan.MortgageLoanStrategy;
import org.springframework.stereotype.Component;

@Component
public class LoanProductText {

  private final MessageService messageService;

  public LoanProductText(MessageService messageService) {
    this.messageService = messageService;
  }

  public String slug(String loanTypeName) {
    if (AutoLoanStrategy.PRODUCT_NAME.equals(loanTypeName)) {
      return "auto";
    }
    if (MortgageLoanStrategy.PRODUCT_NAME.equals(loanTypeName)) {
      return "mortgage";
    }
    return "purpose";
  }

  public String name(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".name");
  }

  public String tag(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".tag");
  }

  public String description(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".description");
  }

  public String amountRange(String minimumAmount, String maximumAmount) {
    return message("loans.amount.range", minimumAmount, maximumAmount);
  }

  public String durationUpTo(Integer durationMonths) {
    return message("loans.duration.upTo", durationMonths);
  }

  public String rateFrom(String rate) {
    return message("loans.rate.from", rate);
  }

  public String remainingAmount(String loanTypeName, String remainingAmount) {
    return message("loans.remaining", name(loanTypeName), remainingAmount);
  }

  private String keyPrefix(String loanTypeName) {
    return "loans." + slug(loanTypeName);
  }

  private String message(String code, Object... args) {
    return messageService.get(code, args);
  }
}
