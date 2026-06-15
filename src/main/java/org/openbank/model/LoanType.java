package org.openbank.model;

import java.math.BigDecimal;
import java.util.Objects;

public class LoanType {

  private Long loanTypeId;
  private String name;
  private BigDecimal rate;
  private Integer duration;
  private BigDecimal minimumAmount;
  private BigDecimal maximumAmount;
  private Long currencyId;

  public LoanType(Long loanTypeId, String name, BigDecimal rate, Integer duration, BigDecimal minimumAmount, BigDecimal maximumAmount, Long currencyId) {
    this.loanTypeId = loanTypeId;
    this.name = name;
    this.rate = rate;
    this.duration = duration;
    this.minimumAmount = minimumAmount;
    this.maximumAmount = maximumAmount;
    this.currencyId = currencyId;
  }

  public Long getLoanTypeId() {
    return loanTypeId;
  }

  public void setLoanTypeId(Long loanTypeId) {
    this.loanTypeId = loanTypeId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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

  public BigDecimal getMinimumAmount() {
    return minimumAmount;
  }

  public void setMinimumAmount(BigDecimal minimumAmount) {
    this.minimumAmount = minimumAmount;
  }

  public BigDecimal getMaximumAmount() {
    return maximumAmount;
  }

  public void setMaximumAmount(BigDecimal maximumAmount) {
    this.maximumAmount = maximumAmount;
  }

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LoanType loanType)) return false;
    return Objects.equals(getLoanTypeId(), loanType.getLoanTypeId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getLoanTypeId());
  }

  @Override
  public String toString() {
    return "LoanType{" +
        "loanTypeId=" + loanTypeId +
        ", name='" + name + '\'' +
        ", rate=" + rate +
        ", duration=" + duration +
        ", minimumAmount=" + minimumAmount +
        ", maximumAmount=" + maximumAmount +
        ", currencyId=" + currencyId +
        '}';
  }
}
