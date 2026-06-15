package org.openbank.dao.loantype;


import org.openbank.model.LoanType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Defines the loan type dao contract.
 */
public interface LoanTypeDao {

  /** Creates a loan type. */
  boolean createNewTypeOfLoan(String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId);

  /** Finds a loan type by id. */
  Optional<LoanType> getLoanTypeById(Long loanTypeId);

  /** Returns all loan types. */
  List<LoanType> getAllLoanTypes();

  /** Updates a loan type rate. */
  boolean changeRateOfLoanType(Long loanTypeId, BigDecimal newRate);

}
