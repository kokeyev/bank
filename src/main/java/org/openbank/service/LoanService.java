package org.openbank.service;

import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Defines loan application, offer, and calculation operations.
 */
public interface LoanService {

  /** Returns all configured loan products. */
  List<LoanType> getAllLoanTypes();

  /** Finds a loan product by display name. */
  Optional<LoanType> getLoanTypeByName(String name);

  /** Finds a loan product by id. */
  Optional<LoanType> getLoanTypeById(Long loanTypeId);

  /** Returns loans created for a user. */
  List<Loan> getLoansByUserId(Long userId);

  /** Returns active loans for a user. */
  List<Loan> getActiveLoansByUserId(Long userId);

  /** Returns loan applications waiting for review. */
  List<Loan> getPendingLoans();

  /** Creates a pending loan application. */
  boolean createApplication(Long userId, String loanTypeName, LoanApplicationRequest request);

  /** Creates a manager offer for a pending application. */
  boolean createOffer(Long parentLoanId, LoanOfferRequest request);

  /** Accepts a manager loan offer. */
  boolean acceptOffer(Long userId, Long loanId);

  /** Rejects a manager loan offer. */
  boolean rejectOffer(Long userId, Long loanId);

  /** Rejects a pending loan application. */
  boolean rejectApplication(Long loanId);

  /** Calculates the current late-payment penalty. */
  BigDecimal calculateLatePenalty(Loan loan);

  /** Calculates expected payment due dates. */
  List<LocalDate> getPaymentDueDates(Loan loan);

  /** Calculates an annuity monthly payment. */
  BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration);
}
