package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OpenDepositRequest {

  @NotNull(message = "{validation.senderAccount.required}")
  private Long sourceAccountId;

  @NotNull(message = "{validation.depositType.required}")
  private Long depositTypeId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
  private BigDecimal amount;
  private Boolean autoRenewal;
  private Boolean reinvestInterest;

  public Long getSourceAccountId() {
    return sourceAccountId;
  }

  public void setSourceAccountId(Long sourceAccountId) {
    this.sourceAccountId = sourceAccountId;
  }

  public Long getDepositTypeId() {
    return depositTypeId;
  }

  public void setDepositTypeId(Long depositTypeId) {
    this.depositTypeId = depositTypeId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Boolean getAutoRenewal() {
    return autoRenewal;
  }

  public void setAutoRenewal(Boolean autoRenewal) {
    this.autoRenewal = autoRenewal;
  }

  public Boolean getReinvestInterest() {
    return reinvestInterest;
  }

  public void setReinvestInterest(Boolean reinvestInterest) {
    this.reinvestInterest = reinvestInterest;
  }
}
