package org.openbank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Deposit {

  private Long depositId;
  private Long userId;
  private Long depositTypeId;
  private Boolean reinvestInterest;
  private Boolean autoRenewal;
  private String status;
  private LocalDate startDate;
  private BigDecimal currentAmount;

  public Deposit(Long depositId, Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, String status, LocalDate startDate, BigDecimal currentAmount) {
    this.depositId = depositId;
    this.userId = userId;
    this.depositTypeId = depositTypeId;
    this.reinvestInterest = reinvestInterest;
    this.autoRenewal = autoRenewal;
    this.status = status;
    this.startDate = startDate;
    this.currentAmount = currentAmount;
  }

  public Long getDepositId() {
    return depositId;
  }

  public void setDepositId(Long depositId) {
    this.depositId = depositId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getDepositTypeId() {
    return depositTypeId;
  }

  public void setDepositTypeId(Long depositTypeId) {
    this.depositTypeId = depositTypeId;
  }

  public Boolean getReinvestInterest() {
    return reinvestInterest;
  }

  public void setReinvestInterest(Boolean reinvestInterest) {
    this.reinvestInterest = reinvestInterest;
  }

  public Boolean getAutoRenewal() {
    return autoRenewal;
  }

  public void setAutoRenewal(Boolean autoRenewal) {
    this.autoRenewal = autoRenewal;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public BigDecimal getCurrentAmount() {
    return currentAmount;
  }

  public void setCurrentAmount(BigDecimal currentAmount) {
    this.currentAmount = currentAmount;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Deposit deposit)) return false;
    return Objects.equals(getDepositId(), deposit.getDepositId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getDepositId());
  }

  @Override
  public String toString() {
    return "Deposit{" +
        "depositId=" + depositId +
        ", userId=" + userId +
        ", depositTypeId=" + depositTypeId +
        ", reinvestInterest=" + reinvestInterest +
        ", autoRenewal=" + autoRenewal +
        ", status='" + status + '\'' +
        ", startDate=" + startDate +
        ", currentAmount=" + currentAmount +
        '}';
  }
}
