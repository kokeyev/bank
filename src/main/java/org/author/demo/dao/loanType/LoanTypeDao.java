package org.author.demo.dao.loanType;


import org.author.demo.model.LoanType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface LoanTypeDao {

  // сомнительно ? Менеджер будет создавать новые типы кредитов ?
  boolean createNewTypeOfLoan(String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId);

  Optional<LoanType> getLoanTypeById(Long loanTypeId);

  List<LoanType> getAllLoanTypes();

  boolean changeRateOfLoanType(Long loanTypeId, BigDecimal newRate);

}
