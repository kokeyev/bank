package org.openbank.service.strategy.loan;

import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

/**
 * Strategy for the auto loan product.
 */
@Component
public class AutoLoanStrategy extends AbstractLoanProductStrategy {

  public static final String PRODUCT_NAME = "Автокредит";

  public AutoLoanStrategy(MessageService messageService) {
    super(messageService);
  }

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
