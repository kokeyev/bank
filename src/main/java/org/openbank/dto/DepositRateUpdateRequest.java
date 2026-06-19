package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DepositRateUpdateRequest {

  @NotNull(message = "{validation.depositType.required}")
  private Long depositTypeId;

  @NotNull(message = "{validation.rate.required}")
  @DecimalMin(value = "0.01", message = "{validation.rate.positive}")
  private BigDecimal rate;

  public Long getDepositTypeId() {
    return depositTypeId;
  }

  public void setDepositTypeId(Long depositTypeId) {
    this.depositTypeId = depositTypeId;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }
}
