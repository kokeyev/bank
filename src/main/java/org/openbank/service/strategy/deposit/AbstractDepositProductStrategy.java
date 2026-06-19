package org.openbank.service.strategy.deposit;

import org.openbank.model.Deposit;
import org.openbank.model.DepositType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Provides default deposit behavior shared by most product families.
 *
 * <p>Concrete strategies override only the rules that differ for a product, which keeps the
 * product-specific classes small and easy to compare.</p>
 */
public abstract class AbstractDepositProductStrategy implements DepositProductStrategy {

  @Override
  public void validateOpeningAmount(DepositType depositType, BigDecimal amount) {
    if (depositType.getMinimumAmount() != null && amount.compareTo(depositType.getMinimumAmount()) < 0) {
      throw new IllegalArgumentException("Минимальная сумма для выбранных условий: " + depositType.getMinimumAmount());
    }
  }

  @Override
  public boolean resolveAutoRenewal(Boolean requestedAutoRenewal) {
    return Boolean.TRUE.equals(requestedAutoRenewal);
  }

  @Override
  public boolean resolveReinvestInterest(Boolean requestedReinvestInterest) {
    return Boolean.TRUE.equals(requestedReinvestInterest);
  }

  @Override
  public boolean canTopUp() {
    return true;
  }

  @Override
  public boolean canWithdraw(DepositType depositType) {
    return Boolean.TRUE.equals(depositType.getWithdrawal());
  }

  @Override
  public BigDecimal calculateMonthlyInterest(Deposit deposit, DepositType depositType) {
    return deposit.getCurrentAmount()
        .multiply(depositType.getRate())
        .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP)
        .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
  }

  @Override
  public LocalDate maturityDate(Deposit deposit, DepositType depositType) {
    if (deposit.getStartDate() == null || depositType.getDuration() == null) {
      return null;
    }
    return deposit.getStartDate().plusMonths(depositType.getDuration());
  }
}
