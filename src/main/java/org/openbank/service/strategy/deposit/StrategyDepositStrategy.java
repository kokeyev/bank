package org.openbank.service.strategy.deposit;

import org.openbank.model.DepositType;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

/**
 * Strategy for the Strategy deposit product.
 * The product currently uses shared deposit defaults while remaining isolated for future
 * product-specific rules.
 */
@Component
public class StrategyDepositStrategy extends AbstractDepositProductStrategy {

  public static final String PRODUCT_NAME = "Стратегия";

  public StrategyDepositStrategy(MessageService messageService) {
    super(messageService);
  }

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }

  @Override
  public boolean canWithdraw(DepositType depositType) {
    return false;
  }
}
