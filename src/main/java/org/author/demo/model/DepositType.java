package org.author.demo.model;

import java.math.BigDecimal;
import java.util.Objects;

public class DepositType {

  private Long depositTypeId;
  private String name;
  private BigDecimal rate;
  private Integer duration;
  private Boolean withdrawal;
  private BigDecimal minimumAmount;
  private Long currencyId;

  public DepositType(Long depositTypeId, String name, BigDecimal rate, Integer duration, Boolean withdrawal, BigDecimal minimumAmount, Long currencyId) {
    this.depositTypeId = depositTypeId;
    this.name = name;
    this.rate = rate;
    this.duration = duration;
    this.withdrawal = withdrawal;
    this.minimumAmount = minimumAmount;
    this.currencyId = currencyId;
  }

  public Long getDepositTypeId() {
    return depositTypeId;
  }

  public void setDepositTypeId(Long depositTypeId) {
    this.depositTypeId = depositTypeId;
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

  public Boolean getWithdrawal() {
    return withdrawal;
  }

  public void setWithdrawal(Boolean withdrawal) {
    this.withdrawal = withdrawal;
  }

  public BigDecimal getMinimumAmount() {
    return minimumAmount;
  }

  public void setMinimumAmount(BigDecimal minimumAmount) {
    this.minimumAmount = minimumAmount;
  }

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DepositType that)) return false;
    return Objects.equals(getDepositTypeId(), that.getDepositTypeId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getDepositTypeId());
  }

  @Override
  public String toString() {
    return "DepositType{" +
        "depositTypeId=" + depositTypeId +
        ", name='" + name + '\'' +
        ", rate=" + rate +
        ", duration=" + duration +
        ", withdrawal=" + withdrawal +
        ", minimumAmount=" + minimumAmount +
        ", currencyId=" + currencyId +
        '}';
  }
}
