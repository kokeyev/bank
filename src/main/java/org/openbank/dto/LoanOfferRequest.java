package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanOfferRequest {

  @NotNull(message = "Offer amount is required")
  @DecimalMin(value = "0.01", message = "Offer amount must be positive")
  private BigDecimal amount;

  @NotNull(message = "Rate is required")
  @DecimalMin(value = "0.01", message = "Rate must be positive")
  private BigDecimal rate;

  @NotNull(message = "Duration is required")
  @Min(value = 1, message = "Duration must be at least 1 month")
  private Integer duration;

  @DecimalMin(value = "0.01", message = "Monthly payment must be positive")
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
