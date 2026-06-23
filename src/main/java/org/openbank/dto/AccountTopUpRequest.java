package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AccountTopUpRequest {

  @NotNull(message = "{validation.targetAccount.required}")
  private Long accountId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
  private BigDecimal amount;

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
