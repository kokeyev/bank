package org.author.demo.dao.loan;

import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoanDao {

  List<Loan> getPendingLoans();

  boolean createOffer(Long userId, Long loanTypeId, BigDecimal remainingAmount, String status, LocalDate startDate, BigDecimal monthlyPayment);

  Optional<List<Loan>> getOffers(Long userId);

  boolean acceptOffer(Long loanId);

  void refuseOffer(Long loanId);

}
