package org.author.demo.dao.loan;

import org.author.demo.model.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanDao {

  List<Loan> getPendingLoans();

  Optional<Loan> getLoanById(Long loanId);

  List<Loan> getLoansByUserId(Long userId);

  List<Loan> getActiveLoansByUserId(Long userId);

  boolean createPendingLoan(Long userId, Long loanTypeId, BigDecimal requestedAmount);

  boolean createOffer(Long parentLoanId, Long userId, Long loanTypeId, BigDecimal amount, BigDecimal rate, Integer duration, BigDecimal monthlyPayment);

  List<Loan> getOffers(Long userId);

  boolean acceptOffer(Long userId, Long loanId);

  boolean refuseOffer(Long userId, Long loanId);

  boolean rejectPendingLoan(Long loanId);

  boolean payLoan(Long loanId, BigDecimal amount);

}
