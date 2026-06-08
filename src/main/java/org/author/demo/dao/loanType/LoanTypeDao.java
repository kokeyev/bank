package org.author.demo.dao.loanType;


import java.math.BigDecimal;

public interface LoanTypeDao {
  boolean createNewTypeOfLoan(String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId);



}
