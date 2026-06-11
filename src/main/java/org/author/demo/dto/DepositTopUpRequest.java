package org.author.demo.dto;

import java.math.BigDecimal;

public class DepositTopUpRequest {

  private Long sourceAccountId;
  private Long depositId;
  private BigDecimal amount;

  public Long getSourceAccountId() {
    return sourceAccountId;
  }

  public void setSourceAccountId(Long sourceAccountId) {
    this.sourceAccountId = sourceAccountId;
  }

  public Long getDepositId() {
    return depositId;
  }

  public void setDepositId(Long depositId) {
    this.depositId = depositId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
