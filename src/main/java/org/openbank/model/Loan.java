package org.openbank.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class Loan {

  private Long loanId;
  private Long userId;
  private Long loanTypeId;
  private Long parentLoanId;
  private BigDecimal remainingAmount;
  private BigDecimal rate;
  private Integer duration;
  private String status;
  private LocalDate startDate;
  private BigDecimal monthlyPayment;

  public Loan(Long loanId, Long userId, Long loanTypeId, Long parentLoanId, BigDecimal remainingAmount, BigDecimal rate, Integer duration, String status, LocalDate startDate, BigDecimal monthlyPayment) {
    this.loanId = loanId;
    this.userId = userId;
    this.loanTypeId = loanTypeId;
    this.parentLoanId = parentLoanId;
    this.remainingAmount = remainingAmount;
    this.rate = rate;
    this.duration = duration;
    this.status = status;
    this.startDate = startDate;
    this.monthlyPayment = monthlyPayment;
  }

  public Long getLoanId() {
    return loanId;
  }

  public void setLoanId(Long loanId) {
    this.loanId = loanId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getLoanTypeId() {
    return loanTypeId;
  }

  public void setLoanTypeId(Long loanTypeId) {
    this.loanTypeId = loanTypeId;
  }

  public Long getParentLoanId() {
    return parentLoanId;
  }

  public void setParentLoanId(Long parentLoanId) {
    this.parentLoanId = parentLoanId;
  }

  public BigDecimal getRemainingAmount() {
    return remainingAmount;
  }

  public void setRemainingAmount(BigDecimal remainingAmount) {
    this.remainingAmount = remainingAmount;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }

  public Integer getDuration() {
    return duration;
  }

  public void setDuration(Integer duration) {
    this.duration = duration;
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

  public BigDecimal getMonthlyPayment() {
    return monthlyPayment;
  }

  public void setMonthlyPayment(BigDecimal monthlyPayment) {
    this.monthlyPayment = monthlyPayment;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Loan loan)) return false;
    return Objects.equals(getLoanId(), loan.getLoanId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getLoanId());
  }

  @Override
  public String toString() {
    return "Loan{" +
        "loanId=" + loanId +
        ", userId=" + userId +
        ", loanTypeId=" + loanTypeId +
        ", parentLoanId=" + parentLoanId +
        ", remainingAmount=" + remainingAmount +
        ", rate=" + rate +
        ", duration=" + duration +
        ", status='" + status + '\'' +
        ", startDate=" + startDate +
        ", monthlyPayment=" + monthlyPayment +
        '}';
  }
}
