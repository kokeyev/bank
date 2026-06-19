package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanPaymentRequest {

  @NotNull(message = "{validation.senderAccount.required}")
  private Long sourceAccountId;

  @NotNull(message = "{validation.loan.required}")
  private Long loanId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
  private BigDecimal amount;

  public Long getSourceAccountId() {
    return sourceAccountId;
  }

  public void setSourceAccountId(Long sourceAccountId) {
    this.sourceAccountId = sourceAccountId;
  }

  public Long getLoanId() {
    return loanId;
  }

  public void setLoanId(Long loanId) {
    this.loanId = loanId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
