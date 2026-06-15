package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class OpenAccountRequest {

  @NotBlank(message = "Account name is required")
  @Size(max = 100, message = "Account name must be shorter than 100 characters")
  private String accountName;

  @NotBlank(message = "Currency is required")
  private String currency;

  @NotNull(message = "Transaction limit is required")
  @DecimalMin(value = "0.00", message = "Transaction limit cannot be negative")
  private BigDecimal transactionLimit;

  public String getAccountName() {
    return accountName;
  }

  public void setAccountName(String accountName) {
    this.accountName = accountName;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public BigDecimal getTransactionLimit() {
    return transactionLimit;
  }

  public void setTransactionLimit(BigDecimal transactionLimit) {
    this.transactionLimit = transactionLimit;
  }
}
