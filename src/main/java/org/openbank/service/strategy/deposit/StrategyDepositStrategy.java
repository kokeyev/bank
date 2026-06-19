package org.openbank.service.strategy.deposit;

import org.springframework.stereotype.Component;

/**
 * Strategy for the Strategy deposit product.
 *
 * <p>The product currently uses shared deposit defaults while remaining isolated for future
 * product-specific rules.</p>
 */
@Component
public class StrategyDepositStrategy extends AbstractDepositProductStrategy {

  public static final String PRODUCT_NAME = "Стратегия";

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }
}
