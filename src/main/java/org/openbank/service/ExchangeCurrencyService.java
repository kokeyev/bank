package org.openbank.service;

import org.openbank.dao.currency.CurrencyDao;
import org.openbank.model.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Provides exchange currency service operations.
 */
@Service
public class ExchangeCurrencyService {

  private final CurrencyDao currencyDao;

  /**
   * Handles exchange currency service.
   */
  public ExchangeCurrencyService(CurrencyDao currencyDao) {
    this.currencyDao = currencyDao;
  }

  /**
   * Handles get all currencies.
   */
  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  /**
   * Handles calculate.
   */
  public BigDecimal calculate(Long fromCurrencyId, Long toCurrencyId, BigDecimal amount) {
    if (fromCurrencyId == null || toCurrencyId == null) {
      throw new IllegalArgumentException("Выберите валюты");
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Введите сумму больше нуля");
    }
    if (fromCurrencyId.equals(toCurrencyId)) {
      return amount;
    }

    BigDecimal fromRate = currencyDao.getCurrencyRateToKztById(fromCurrencyId);
    BigDecimal toRate = currencyDao.getCurrencyRateToKztById(toCurrencyId);
    return amount.multiply(fromRate).divide(toRate, 2, RoundingMode.HALF_UP);
  }

  /**
   * Handles update rate.
   */
  public boolean updateRate(Long currencyId, BigDecimal rateToKzt) {
    if (currencyId == null) {
      throw new IllegalArgumentException("Выберите валюту");
    }
    if (rateToKzt == null || rateToKzt.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Введите курс больше нуля");
    }
    return currencyDao.updateCurrencyRate(currencyId, rateToKzt);
  }
}
