package org.openbank.service.strategy.loan;

import org.springframework.stereotype.Component;

/**
 * Strategy for the auto loan product.
 */
@Component
public class AutoLoanStrategy extends AbstractLoanProductStrategy {

  public static final String PRODUCT_NAME = "Автокредит";

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
