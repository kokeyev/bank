package org.openbank.service.strategy.deposit;

import org.springframework.stereotype.Component;

/**
 * Strategy for the Kopilka deposit product.
 *
 * <p>The product currently uses the shared deposit defaults for top-up, withdrawal, interest, and
 * maturity behavior.</p>
 */
@Component
public class KopilkaDepositStrategy extends AbstractDepositProductStrategy {

  public static final String PRODUCT_NAME = "Копилка";

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
