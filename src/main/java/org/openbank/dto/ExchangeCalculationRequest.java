package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ExchangeCalculationRequest {

  @NotNull(message = "{exchange.validation.currencies.required}")
  private Long fromCurrencyId;

  @NotNull(message = "{exchange.validation.currencies.required}")
  private Long toCurrencyId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
  private BigDecimal amount;

  public Long getFromCurrencyId() {
    return fromCurrencyId;
  }

  public void setFromCurrencyId(Long fromCurrencyId) {
    this.fromCurrencyId = fromCurrencyId;
  }

  public Long getToCurrencyId() {
    return toCurrencyId;
  }

  public void setToCurrencyId(Long toCurrencyId) {
    this.toCurrencyId = toCurrencyId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
