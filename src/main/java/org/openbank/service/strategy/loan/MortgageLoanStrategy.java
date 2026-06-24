package org.openbank.service.strategy.loan;

import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

/**
 * Strategy for the mortgage loan product.
 */
@Component
public class MortgageLoanStrategy extends AbstractLoanProductStrategy {

  public static final String PRODUCT_NAME = "Ипотека";

  public MortgageLoanStrategy(MessageService messageService) {
    super(messageService);
  }

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
