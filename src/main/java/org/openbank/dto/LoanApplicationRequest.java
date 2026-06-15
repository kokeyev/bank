package org.openbank.dto;

import java.math.BigDecimal;

public class LoanApplicationRequest {

  private BigDecimal amount;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
