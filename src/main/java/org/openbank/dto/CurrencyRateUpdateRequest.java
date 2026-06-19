package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CurrencyRateUpdateRequest {

  @NotNull(message = "{validation.currency.required}")
  private Long currencyId;

  @NotNull(message = "{validation.rate.required}")
  @DecimalMin(value = "0.0001", message = "{exchange.validation.rate.positive}")
  private BigDecimal rateToKzt;

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  public BigDecimal getRateToKzt() {
    return rateToKzt;
  }

  public void setRateToKzt(BigDecimal rateToKzt) {
    this.rateToKzt = rateToKzt;
  }
}
