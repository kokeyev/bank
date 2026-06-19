package org.openbank.service;

import org.openbank.dao.loan.LoanDao;
import org.openbank.dao.loantype.LoanTypeDao;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.strategy.loan.LoanProductStrategy;
import org.openbank.service.strategy.loan.LoanProductStrategyResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies loan application and offer rules before loan data is persisted.
 *
 * <p>Product-specific calculations and constraints are resolved through
 * {@link LoanProductStrategy}, while this service validates request flow and status transitions.</p>
 */
@Service
public class LoanService {

  private final LoanDao loanDao;
  private final LoanTypeDao loanTypeDao;
  private final LoanProductStrategyResolver strategyResolver;
  public LoanService(LoanDao loanDao, LoanTypeDao loanTypeDao, LoanProductStrategyResolver strategyResolver) {
    this.loanDao = loanDao;
    this.loanTypeDao = loanTypeDao;
    this.strategyResolver = strategyResolver;
  }

  /**
   * Returns all configured loan products.
   *
   * @return loan type records available for applications and manager offers
   */
  public List<LoanType> getAllLoanTypes() {
    return loanTypeDao.getAllLoanTypes();
  }

  /**
   * Finds a loan product by its display name.
   *
   * @param name product name stored in the database
   * @return matching loan type when present
   */
  public Optional<LoanType> getLoanTypeByName(String name) {
    for (LoanType loanType : loanTypeDao.getAllLoanTypes()) {
      if (loanType.getName().equals(name)) {
        return Optional.of(loanType);
      }
    }
    return Optional.empty();
  }

  /**
   * Finds a loan product by identifier.
   *
   * @param loanTypeId loan type primary key
   * @return matching loan type when present
   */
  public Optional<LoanType> getLoanTypeById(Long loanTypeId) {
    return loanTypeDao.getLoanTypeById(loanTypeId);
  }

  /**
   * Loads every loan record created for a user.
   *
   * @param userId owner identifier
   * @return loans belonging to the user
   */
  public List<Loan> getLoansByUserId(Long userId) {
    return loanDao.getLoansByUserId(userId);
  }

  /**
   * Loads active loans for repayment views.
   *
   * @param userId owner identifier
   * @return active user loans
   */
  public List<Loan> getActiveLoansByUserId(Long userId) {
    return loanDao.getActiveLoansByUserId(userId);
  }

  /**
   * Loads loan applications waiting for manager review.
   *
   * @return pending loan applications
   */
  public List<Loan> getPendingLoans() {
    return loanDao.getPendingLoans();
  }

  /**
   * Creates a pending loan application after product amount validation.
   *
   * @param userId applicant identifier
   * @param loanTypeName requested product name
   * @param request application amount
   * @return {@code true} when the application is stored
   * @throws IllegalArgumentException when the product is missing or amount violates product rules
   */
  public boolean createApplication(Long userId, String loanTypeName, LoanApplicationRequest request) {
    validatePositive(request.getAmount(), "Введите сумму кредита");

    Optional<LoanType> loanTypeOptional = getLoanTypeByName(loanTypeName);
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalArgumentException("Тип кредита не найден");
    }
    LoanType loanType = loanTypeOptional.get();
    LoanProductStrategy strategy = strategyResolver.resolve(loanType);

    strategy.validateApplicationAmount(loanType, request.getAmount());

    return loanDao.createPendingLoan(userId, loanType.getLoanTypeId(), request.getAmount());
  }

  /**
   * Creates a manager offer for a pending loan application.
   *
   * @param parentLoanId original pending application
   * @param request proposed amount, rate, duration, and optional monthly payment
   * @return {@code true} when the offer is stored
   * @throws IllegalArgumentException when the application is missing, not pending, or violates product rules
   */
  public boolean createOffer(Long parentLoanId, LoanOfferRequest request) {
    validatePositive(request.getAmount(), "Введите сумму предложения");
    validatePositive(request.getRate(), "Введите процентную ставку");

    if (request.getDuration() == null || request.getDuration() <= 0) {
      throw new IllegalArgumentException("Введите срок кредита");
    }

    Optional<Loan> loanOptional = loanDao.getLoanById(parentLoanId);
    if (loanOptional.isEmpty()) {
      throw new IllegalArgumentException("Заявка не найдена");
    }
    Loan parentLoan = loanOptional.get();

    if (!LoanStatus.PENDING.name().equals(parentLoan.getStatus())) {
      throw new IllegalArgumentException("Предложения можно создавать только для заявок в статусе PENDING");
    }

    Optional<LoanType> loanTypeOptional = loanTypeDao.getLoanTypeById(parentLoan.getLoanTypeId());
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalArgumentException("Тип кредита не найден");
    }
    LoanType loanType = loanTypeOptional.get();
    LoanProductStrategy strategy = strategyResolver.resolve(loanType);

    strategy.validateOffer(loanType, request.getAmount(), request.getDuration());

    BigDecimal monthlyPayment = request.getMonthlyPayment();
    if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
      monthlyPayment = strategy.calculateMonthlyPayment(request.getAmount(), request.getRate(), request.getDuration());
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

  /**
   * Accepts a manager loan offer on behalf of the owner.
   *
   * @param userId expected loan owner
   * @param loanId offer identifier
   * @return {@code true} when the offer becomes active
   */
  public boolean acceptOffer(Long userId, Long loanId) {
    return loanDao.acceptOffer(userId, loanId);
  }

  /**
   * Rejects a manager loan offer on behalf of the owner.
   *
   * @param userId expected loan owner
   * @param loanId offer identifier
   * @return {@code true} when the offer is refused
   */
  public boolean rejectOffer(Long userId, Long loanId) {
    return loanDao.refuseOffer(userId, loanId);
  }

  /**
   * Rejects a pending loan application during manager review.
   *
   * @param loanId pending application identifier
   * @return {@code true} when the application is rejected
   */
  public boolean rejectApplication(Long loanId) {
    return loanDao.rejectPendingLoan(loanId);
  }

  /**
   * Calculates the current late-payment penalty for a loan.
   *
   * @param loan loan to inspect; {@code null} returns zero
   * @return calculated penalty or zero when the product cannot be resolved
   */
  public BigDecimal calculateLatePenalty(Loan loan) {
    if (loan == null || loan.getLoanTypeId() == null) {
      return BigDecimal.ZERO;
    }

    return loanTypeDao.getLoanTypeById(loan.getLoanTypeId())
        .map(strategyResolver::resolve)
        .map(strategy -> strategy.calculateLatePenalty(loan))
        .orElse(BigDecimal.ZERO);
  }

  /**
   * Calculates expected payment due dates for a loan.
   *
   * @param loan active or offered loan; {@code null} returns an empty list
   * @return due dates calculated by the product strategy
   */
  public List<LocalDate> getPaymentDueDates(Loan loan) {
    if (loan == null || loan.getLoanTypeId() == null) {
      return List.of();
    }

    return loanTypeDao.getLoanTypeById(loan.getLoanTypeId())
        .map(strategyResolver::resolve)
        .map(strategy -> strategy.getPaymentDueDates(loan))
        .orElse(List.of());
  }

  /**
   * Calculates an annuity monthly payment with the default loan strategy.
   *
   * @param amount principal amount
   * @param annualRate yearly percent rate
   * @param duration duration in months
   * @return rounded monthly payment
   */
  public BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration) {
    return strategyResolver.defaultStrategy().calculateMonthlyPayment(amount, annualRate, duration);
  }

  private void validatePositive(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(message);
    }
  }
}
