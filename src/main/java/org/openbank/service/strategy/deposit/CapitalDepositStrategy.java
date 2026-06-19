package org.openbank.service.strategy.deposit;

import org.openbank.model.DepositType;
import org.springframework.stereotype.Component;

/**
 * Strategy for the Capital deposit product.
 *
 * <p>Capital deposits lock funds until maturity, always reinvest interest, and disable manual
 * top-up and withdrawal.</p>
 */
@Component
public class CapitalDepositStrategy extends AbstractDepositProductStrategy {

  public static final String PRODUCT_NAME = "Капитал";

  @Override
  public String productName() {
    return PRODUCT_NAME;
  }

  @Override
  public boolean resolveAutoRenewal(Boolean requestedAutoRenewal) {
    return false;
  }

  @Override
  public boolean resolveReinvestInterest(Boolean requestedReinvestInterest) {
    return true;
  }

  @Override
  public boolean canTopUp() {
    return false;
  }

  @Override
  public boolean canWithdraw(DepositType depositType) {
    return false;
  }
}
