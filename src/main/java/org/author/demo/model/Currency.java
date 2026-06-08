package org.author.demo.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Currency {

  private Long currencyId;
  private String name;
  private BigDecimal rateToKzt;

  public Currency(Long currencyId, String name, BigDecimal rateToKzt) {
    this.currencyId = currencyId;
    this.name = name;
    this.rateToKzt = rateToKzt;
  }

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getRateToKzt() {
    return rateToKzt;
  }

  public void setRateToKzt(BigDecimal rateToKzt) {
    this.rateToKzt = rateToKzt;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Currency currency)) return false;
    return Objects.equals(getCurrencyId(), currency.getCurrencyId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getCurrencyId());
  }

  @Override
  public String toString() {
    return "Currency{" +
        "currencyId=" + currencyId +
        ", name='" + name + '\'' +
        ", rateToKzt=" + rateToKzt +
        '}';
  }
}
