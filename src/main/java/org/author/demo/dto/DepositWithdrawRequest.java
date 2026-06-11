package org.author.demo.dto;

import java.math.BigDecimal;

public class DepositWithdrawRequest {

  private Long depositId;
  private Long targetAccountId;
  private BigDecimal amount;

  public Long getDepositId() {
    return depositId;
  }

  public void setDepositId(Long depositId) {
    this.depositId = depositId;
  }

  public Long getTargetAccountId() {
    return targetAccountId;
  }

  public void setTargetAccountId(Long targetAccountId) {
    this.targetAccountId = targetAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
