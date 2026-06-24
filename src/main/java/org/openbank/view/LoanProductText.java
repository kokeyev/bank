package org.openbank.view;

import org.openbank.service.MessageService;
import org.openbank.service.strategy.loan.AutoLoanStrategy;
import org.openbank.service.strategy.loan.MortgageLoanStrategy;
import org.openbank.service.strategy.loan.PurposeLoanStrategy;
import org.springframework.stereotype.Component;

@Component
public class LoanProductText {

  private final MessageService messageService;

  public LoanProductText(MessageService messageService) {
    this.messageService = messageService;
  }

  public String urlPath(String loanTypeName) {
    return product(loanTypeName).urlPath;
  }

  public String name(String loanTypeName) {
    return messageService.get(product(loanTypeName).messagePrefix + ".name");
  }

  public String tag(String loanTypeName) {
    return messageService.get(product(loanTypeName).messagePrefix + ".tag");
  }

  public String description(String loanTypeName) {
    return messageService.get(product(loanTypeName).messagePrefix + ".description");
  }

  public String amountRange(String minimumAmount, String maximumAmount) {
    return messageService.get("loans.amount.range", minimumAmount, maximumAmount);
  }

  public String durationUpTo(Integer durationMonths) {
    return messageService.get("loans.duration.upTo", durationMonths);
  }

  public String rateFrom(String rate) {
    return messageService.get("loans.rate.from", rate);
  }

  public String remainingAmount(String loanTypeName, String remainingAmount) {
    return messageService.get("loans.remaining", name(loanTypeName), remainingAmount);
  }

  private LoanProduct product(String loanTypeName) {
    for (LoanProduct product : LoanProduct.values()) {
      if (product.productName.equals(loanTypeName)) {
        return product;
      }
    }

    return LoanProduct.PURPOSE;
  }

  private enum LoanProduct {
    PURPOSE(PurposeLoanStrategy.PRODUCT_NAME, "purpose", "loans.purpose"),
    AUTO(AutoLoanStrategy.PRODUCT_NAME, "auto", "loans.auto"),
    MORTGAGE(MortgageLoanStrategy.PRODUCT_NAME, "mortgage", "loans.mortgage");

    private final String productName;
    private final String urlPath;
    private final String messagePrefix;

    LoanProduct(String productName, String urlPath, String messagePrefix) {
      this.productName = productName;
      this.urlPath = urlPath;
      this.messagePrefix = messagePrefix;
    }
  }
}
