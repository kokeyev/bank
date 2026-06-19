package org.openbank.service.strategy.deposit;

import org.openbank.model.Deposit;
import org.openbank.model.DepositType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Defines behavior that changes between deposit product families.
 *
 * <p>Each implementation maps to one product name from the deposit type table and decides product
 * options such as auto-renewal, top-up, withdrawal, maturity, and monthly interest.</p>
 */
public interface DepositProductStrategy {

  /**
   * Returns the database product name handled by this strategy.
   *
   * @return product name used in deposit type records
   */
  String productName();

  /**
   * Validates the opening amount against product-specific constraints.
   *
   * @param depositType selected deposit terms
   * @param amount requested opening amount
   * @throws IllegalArgumentException when the amount violates product rules
   */
  void validateOpeningAmount(DepositType depositType, BigDecimal amount);

  /**
   * Resolves whether auto-renewal should be enabled for a new deposit.
   *
   * @param requestedAutoRenewal value submitted by the client
   * @return effective auto-renewal value stored with the deposit
   */
  boolean resolveAutoRenewal(Boolean requestedAutoRenewal);

  /**
   * Resolves whether interest should be added back to the deposit balance.
   *
   * @param requestedReinvestInterest value submitted by the client
   * @return effective reinvestment value stored with the deposit
   */
  boolean resolveReinvestInterest(Boolean requestedReinvestInterest);

  /**
   * Checks whether the product allows additional deposits after opening.
   *
   * @return {@code true} when top-up is allowed
   */
  boolean canTopUp();

  /**
   * Checks whether the product allows withdrawals before maturity.
   *
   * @param depositType selected deposit terms
   * @return {@code true} when withdrawal is allowed
   */
  boolean canWithdraw(DepositType depositType);

  /**
   * Calculates one month of interest for an active deposit.
   *
   * @param deposit active deposit
   * @param depositType product terms for the deposit
   * @return interest amount rounded for money display
   */
  BigDecimal calculateMonthlyInterest(Deposit deposit, DepositType depositType);

  /**
   * Calculates the maturity date for a deposit.
   *
   * @param deposit deposit with a start date
   * @param depositType product terms containing duration
   * @return maturity date, or {@code null} when the product has no fixed maturity
   */
  LocalDate maturityDate(Deposit deposit, DepositType depositType);
}
