package org.openbank.dao.loan;

import org.openbank.model.Loan;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Defines the loan dao contract.
 */
public interface LoanDao {

  /** Returns pending loans. */
  List<Loan> getPendingLoans();

  /** Finds a loan by id. */
  Optional<Loan> getLoanById(Long loanId);

  /** Returns loans for a user. */
  List<Loan> getLoansByUserId(Long userId);

  /** Returns active loans for a user. */
  List<Loan> getActiveLoansByUserId(Long userId);

  /** Creates a pending loan request. */
  boolean createPendingLoan(Long userId, Long loanTypeId, BigDecimal requestedAmount);

  /** Creates a loan offer. */
  boolean createOffer(Long parentLoanId, Long userId, Long loanTypeId, BigDecimal amount, BigDecimal rate, Integer duration, BigDecimal monthlyPayment);

  /** Returns loan offers for a user. */
  List<Loan> getOffers(Long userId);

  /** Accepts a loan offer. */
  boolean acceptOffer(Long userId, Long loanId);

  /** Refuses a loan offer. */
  boolean refuseOffer(Long userId, Long loanId);

  /** Rejects a pending loan request. */
  boolean rejectPendingLoan(Long loanId);

  /** Pays a loan. */
  boolean payLoan(Long loanId, BigDecimal amount);

  /** Pays a loan using an existing transaction. */
  boolean payLoan(Connection connection, Long loanId, BigDecimal amount);

}
