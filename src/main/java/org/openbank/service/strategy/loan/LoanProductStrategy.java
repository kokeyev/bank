package org.openbank.service.strategy.loan;

import org.openbank.model.Loan;
import org.openbank.model.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Defines behavior that changes between loan product families.
 *
 * <p>Strategies validate product limits and calculate payment-related values without mixing those
 * rules into controller or DAO code.</p>
 */
public interface LoanProductStrategy {

  /**
   * Returns the database product name handled by this strategy.
   *
   * @return product name used in loan type records
   */
  String productName();

  /**
   * Validates a client's loan application amount.
   *
   * @param loanType selected product terms
   * @param amount requested principal amount
   * @throws IllegalArgumentException when the amount violates product rules
   */
  void validateApplicationAmount(LoanType loanType, BigDecimal amount);

  /**
   * Validates a manager offer for a pending application.
   *
   * @param loanType selected product terms
   * @param amount offered principal amount
   * @param duration offered duration in months
   * @throws IllegalArgumentException when amount or duration violates product rules
   */
  void validateOffer(LoanType loanType, BigDecimal amount, Integer duration);

  /**
   * Calculates an annuity monthly payment.
   *
   * @param amount principal amount
   * @param annualRate annual percentage rate
   * @param duration duration in months
   * @return rounded monthly payment
   */
  BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration);

  /**
   * Calculates late-payment penalty for an active loan.
   *
   * @param loan active loan to inspect
   * @return penalty amount, or zero when the loan is not overdue
   */
  BigDecimal calculateLatePenalty(Loan loan);

  /**
   * Builds the scheduled due dates for a loan.
   *
   * @param loan loan with start date and duration
   * @return ordered payment due dates
   */
  List<LocalDate> getPaymentDueDates(Loan loan);
}
