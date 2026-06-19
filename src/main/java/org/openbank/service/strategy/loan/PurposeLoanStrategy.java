package org.openbank.service.strategy.loan;

import org.springframework.stereotype.Component;

/**
 * Strategy for general-purpose consumer loans.
 */
@Component
public class PurposeLoanStrategy extends AbstractLoanProductStrategy {

  public static final String PRODUCT_NAME = "На любые цели";

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
