package org.openbank.service.strategy.loan;

import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.LoanStatus;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides shared loan validation and annuity calculation logic.
 *
 * <p>Concrete loan strategies can override these defaults when a product needs special rules while
 * keeping common financial formulas in one place.</p>
 */
public abstract class AbstractLoanProductStrategy implements LoanProductStrategy {

  private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

  @Override
  public void validateApplicationAmount(LoanType loanType, BigDecimal amount) {
    validateAmountRange(loanType, amount);
  }

  @Override
  public void validateOffer(LoanType loanType, BigDecimal amount, Integer duration) {
    validateAmountRange(loanType, amount);
    if (loanType.getDuration() != null && duration != null && duration > loanType.getDuration()) {
      throw new IllegalArgumentException("Срок предложения не должен превышать срок продукта: " + loanType.getDuration());
    }
  }

  @Override
  public BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration) {
    BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), MATH_CONTEXT).divide(BigDecimal.valueOf(12), MATH_CONTEXT);

    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      return amount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
    }

    BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
    BigDecimal ratePower = onePlusRate.pow(duration, MATH_CONTEXT);
    BigDecimal numerator = amount.multiply(monthlyRate, MATH_CONTEXT).multiply(ratePower, MATH_CONTEXT);
    BigDecimal denominator = ratePower.subtract(BigDecimal.ONE, MATH_CONTEXT);

    return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
  }

  @Override
  public BigDecimal calculateLatePenalty(Loan loan) {
    if (loan.getStartDate() == null || loan.getMonthlyPayment() == null || loan.getDuration() == null || !LoanStatus.ACTIVE.name().equals(loan.getStatus())) {
      return BigDecimal.ZERO;
    }

    long passedMonths = Math.max(0, ChronoUnit.MONTHS.between(loan.getStartDate(), LocalDate.now()));
    long expectedPaidMonths = Math.min(passedMonths, loan.getDuration());
    BigDecimal expectedRemaining = loan.getMonthlyPayment().multiply(BigDecimal.valueOf(loan.getDuration() - expectedPaidMonths)).max(BigDecimal.ZERO);

    if (loan.getRemainingAmount() == null || loan.getRemainingAmount().compareTo(expectedRemaining) <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal overdueAmount = loan.getRemainingAmount().subtract(expectedRemaining);
    return overdueAmount.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
  }

  @Override
  public List<LocalDate> getPaymentDueDates(Loan loan) {
    if (loan.getStartDate() == null || loan.getDuration() == null || loan.getDuration() <= 0) {
      return List.of();
    }

    List<LocalDate> dates = new ArrayList<>();
    for (int month = 1; month <= loan.getDuration(); month++) {
      dates.add(loan.getStartDate().plusMonths(month));
    }
    return dates;
  }

  private void validateAmountRange(LoanType loanType, BigDecimal amount) {
    if (loanType.getMinimumAmount() != null && amount.compareTo(loanType.getMinimumAmount()) < 0) {
      throw new IllegalArgumentException("Минимальная сумма кредита: " + loanType.getMinimumAmount());
    }

    if (loanType.getMaximumAmount() != null && amount.compareTo(loanType.getMaximumAmount()) > 0) {
      throw new IllegalArgumentException("Максимальная сумма кредита: " + loanType.getMaximumAmount());
    }
  }
}
