package org.author.demo.dao.currency;

import java.math.BigDecimal;

public interface CurrencyDao {

  String getCurrencyNameById(Long currencyId);

  BigDecimal getCurrencyRateToKztById(Long currencyId);

}
