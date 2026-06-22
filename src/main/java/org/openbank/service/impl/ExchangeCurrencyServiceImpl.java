package org.openbank.service.impl;

import org.openbank.service.ExchangeCurrencyService;
import org.openbank.service.Messages;
import org.openbank.dao.CurrencyDao;
import org.openbank.model.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ExchangeCurrencyServiceImpl implements ExchangeCurrencyService {

  private final CurrencyDao currencyDao;
  public ExchangeCurrencyServiceImpl(CurrencyDao currencyDao) {
    this.currencyDao = currencyDao;
  }

  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  public BigDecimal calculate(Long fromCurrencyId, Long toCurrencyId, BigDecimal amount) {
    if (fromCurrencyId == null || toCurrencyId == null) {
      throw new IllegalArgumentException(Messages.get("exchange.validation.currencies.required"));
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(Messages.get("validation.amount.positive"));
    }
    if (fromCurrencyId.equals(toCurrencyId)) {
      return amount;
    }

    BigDecimal fromRate = currencyDao.getCurrencyRateToKztById(fromCurrencyId);
    BigDecimal toRate = currencyDao.getCurrencyRateToKztById(toCurrencyId);
    return amount.multiply(fromRate).divide(toRate, 2, RoundingMode.HALF_UP);
  }

  public boolean updateRate(Long currencyId, BigDecimal rateToKzt) {
    if (currencyId == null) {
      throw new IllegalArgumentException(Messages.get("validation.currency.required"));
    }
    if (rateToKzt == null || rateToKzt.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(Messages.get("exchange.validation.rate.positive"));
    }
    return currencyDao.updateCurrencyRate(currencyId, rateToKzt);
  }
}
