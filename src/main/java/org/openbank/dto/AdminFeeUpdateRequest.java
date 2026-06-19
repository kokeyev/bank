package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AdminFeeUpdateRequest {

  @NotNull(message = "{validation.fee.required}")
  @DecimalMin(value = "0.00", message = "{validation.fee.min}")
  private BigDecimal transferFeePercent;

  public BigDecimal getTransferFeePercent() {
    return transferFeePercent;
  }

  public void setTransferFeePercent(BigDecimal transferFeePercent) {
    this.transferFeePercent = transferFeePercent;
  }
}
