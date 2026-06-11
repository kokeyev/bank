package org.author.demo.services;

import org.author.demo.dao.loan.LoanDao;
import org.author.demo.dao.loanType.LoanTypeDao;
import org.author.demo.dto.LoanApplicationRequest;
import org.author.demo.dto.LoanOfferRequest;
import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.model.status.LoanStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LoanService {

  private static final MathContext MATH_CONTEXT = new MathContext(16, RoundingMode.HALF_UP);

  private final LoanDao loanDao;
  private final LoanTypeDao loanTypeDao;

  public LoanService(LoanDao loanDao, LoanTypeDao loanTypeDao) {
    this.loanDao = loanDao;
    this.loanTypeDao = loanTypeDao;
  }

  public List<LoanType> getAllLoanTypes() {
    return loanTypeDao.getAllLoanTypes();
  }

  public Optional<LoanType> getLoanTypeByName(String name) {
    return loanTypeDao.getAllLoanTypes()
        .stream()
        .filter(loanType -> loanType.getName().equals(name))
        .findFirst();
  }

  public Optional<LoanType> getLoanTypeById(Long loanTypeId) {
    return loanTypeDao.getLoanTypeById(loanTypeId);
  }

  public List<Loan> getLoansByUserId(Long userId) {
    return loanDao.getLoansByUserId(userId);
  }

  public List<Loan> getActiveLoansByUserId(Long userId) {
    return loanDao.getActiveLoansByUserId(userId);
  }

  public List<Loan> getPendingLoans() {
    return loanDao.getPendingLoans();
  }

  public boolean createApplication(Long userId, String loanTypeName, LoanApplicationRequest request) {
    validatePositive(request.getAmount(), "Введите сумму кредита");

    LoanType loanType = getLoanTypeByName(loanTypeName)
        .orElseThrow(() -> new IllegalArgumentException("Тип кредита не найден"));

    validateAmountRange(loanType, request.getAmount());

    return loanDao.createPendingLoan(userId, loanType.getLoanTypeId(), request.getAmount());
  }

  public boolean createOffer(Long parentLoanId, LoanOfferRequest request) {
    validatePositive(request.getAmount(), "Введите сумму предложения");
    validatePositive(request.getRate(), "Введите процентную ставку");

    if (request.getDuration() == null || request.getDuration() <= 0) {
      throw new IllegalArgumentException("Введите срок кредита");
    }

    Loan parentLoan = loanDao.getLoanById(parentLoanId)
        .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

    if (!LoanStatus.PENDING.name().equals(parentLoan.getStatus())) {
      throw new IllegalArgumentException("Предложения можно создавать только для заявок в статусе PENDING");
    }

    LoanType loanType = loanTypeDao.getLoanTypeById(parentLoan.getLoanTypeId())
        .orElseThrow(() -> new IllegalArgumentException("Тип кредита не найден"));
    validateAmountRange(loanType, request.getAmount());

    BigDecimal monthlyPayment = request.getMonthlyPayment();
    if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
      monthlyPayment = calculateMonthlyPayment(request.getAmount(), request.getRate(), request.getDuration());
    }

    return loanDao.createOffer(
        parentLoan.getLoanId(),
        parentLoan.getUserId(),
        parentLoan.getLoanTypeId(),
        request.getAmount(),
        request.getRate(),
        request.getDuration(),
        monthlyPayment
    );
  }

  public boolean acceptOffer(Long userId, Long loanId) {
    return loanDao.acceptOffer(userId, loanId);
  }

  public boolean rejectOffer(Long userId, Long loanId) {
    return loanDao.refuseOffer(userId, loanId);
  }

  public boolean rejectApplication(Long loanId) {
    return loanDao.rejectPendingLoan(loanId);
  }

  public BigDecimal calculateLatePenalty(Loan loan) {
    if (loan.getStartDate() == null
        || loan.getMonthlyPayment() == null
        || loan.getDuration() == null
        || !LoanStatus.ACTIVE.name().equals(loan.getStatus())) {
      return BigDecimal.ZERO;
    }

    long passedMonths = Math.max(0, java.time.temporal.ChronoUnit.MONTHS.between(loan.getStartDate(), LocalDate.now()));
    long expectedPaidMonths = Math.min(passedMonths, loan.getDuration());
    BigDecimal expectedRemaining = loan.getMonthlyPayment()
        .multiply(BigDecimal.valueOf(loan.getDuration() - expectedPaidMonths))
        .max(BigDecimal.ZERO);

    if (loan.getRemainingAmount() == null || loan.getRemainingAmount().compareTo(expectedRemaining) <= 0) {
      return BigDecimal.ZERO;
    }

    BigDecimal overdueAmount = loan.getRemainingAmount().subtract(expectedRemaining);
    return overdueAmount.multiply(BigDecimal.valueOf(0.01)).setScale(2, RoundingMode.HALF_UP);
  }

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

  public BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration) {
    BigDecimal monthlyRate = annualRate
        .divide(BigDecimal.valueOf(100), MATH_CONTEXT)
        .divide(BigDecimal.valueOf(12), MATH_CONTEXT);

    if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
      return amount.divide(BigDecimal.valueOf(duration), 2, RoundingMode.HALF_UP);
    }

    BigDecimal onePlusRate = BigDecimal.ONE.add(monthlyRate);
    BigDecimal ratePower = onePlusRate.pow(duration, MATH_CONTEXT);
    BigDecimal numerator = amount.multiply(monthlyRate, MATH_CONTEXT).multiply(ratePower, MATH_CONTEXT);
    BigDecimal denominator = ratePower.subtract(BigDecimal.ONE, MATH_CONTEXT);

    return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
  }

  private void validateAmountRange(LoanType loanType, BigDecimal amount) {
    if (loanType.getMinimumAmount() != null && amount.compareTo(loanType.getMinimumAmount()) < 0) {
      throw new IllegalArgumentException("Минимальная сумма кредита: " + loanType.getMinimumAmount());
    }

    if (loanType.getMaximumAmount() != null && amount.compareTo(loanType.getMaximumAmount()) > 0) {
      throw new IllegalArgumentException("Максимальная сумма кредита: " + loanType.getMaximumAmount());
    }
  }

  private void validatePositive(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(message);
    }
  }
}
