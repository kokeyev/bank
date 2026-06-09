package org.author.demo.dao.loanType;


import java.math.BigDecimal;

public interface LoanTypeDao {

  // сомнительно ? Менеджер будет создавать новые типы кредитов ?
  boolean createNewTypeOfLoan(String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId);



}
