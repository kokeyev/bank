package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanOfferRequest {

  @NotNull(message = "{validation.offerAmount.required}")
  @DecimalMin(value = "0.01", message = "{validation.offerAmount.positive}")
  private BigDecimal amount;

  @NotNull(message = "{validation.rate.required}")
  @DecimalMin(value = "0.01", message = "{validation.rate.positive}")
  private BigDecimal rate;

  @NotNull(message = "{validation.duration.required}")
  @Min(value = 1, message = "{validation.duration.min}")
  private Integer duration;

  @DecimalMin(value = "0.01", message = "{validation.monthlyPayment.positive}")
  private BigDecimal monthlyPayment;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
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

  public BigDecimal getMonthlyPayment() {
    return monthlyPayment;
  }

  public void setMonthlyPayment(BigDecimal monthlyPayment) {
    this.monthlyPayment = monthlyPayment;
  }
}
