package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AccountLimitUpdateRequest {

  @NotNull(message = "{validation.transactionLimit.required}")
  @DecimalMin(value = "0.00", message = "{validation.transactionLimit.min}")
  private BigDecimal transactionLimit;

  public BigDecimal getTransactionLimit() {
    return transactionLimit;
  }

  public void setTransactionLimit(BigDecimal transactionLimit) {
    this.transactionLimit = transactionLimit;
  }
}
