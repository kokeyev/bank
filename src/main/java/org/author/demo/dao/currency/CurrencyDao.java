package org.author.demo.dao.currency;

import org.author.demo.model.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CurrencyDao {

  Optional<Currency> getCurrencyById(Long currencyId);

  Optional<Currency> getCurrencyByName(String name);

  List<Currency> getAllCurrencies();

  String getCurrencyNameById(Long currencyId);

  BigDecimal getCurrencyRateToKztById(Long currencyId);

  boolean updateCurrencyRate(Long currencyId, BigDecimal rateToKzt);

}
