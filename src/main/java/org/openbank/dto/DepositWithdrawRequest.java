package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DepositWithdrawRequest {

  private Long depositId;

  @NotNull(message = "{validation.targetAccount.required}")
  private Long targetAccountId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
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
