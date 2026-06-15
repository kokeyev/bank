package org.openbank.dto;

import java.math.BigDecimal;

public class OpenDepositRequest {

  private Long sourceAccountId;
  private Long depositTypeId;
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
