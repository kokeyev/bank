package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanRateUpdateRequest {

  @NotNull(message = "{validation.loanType.required}")
  private Long loanTypeId;

  @NotNull(message = "{validation.rate.required}")
  @DecimalMin(value = "0.01", message = "{validation.rate.positive}")
  private BigDecimal rate;

  public Long getLoanTypeId() {
    return loanTypeId;
  }

  public void setLoanTypeId(Long loanTypeId) {
    this.loanTypeId = loanTypeId;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }
}
