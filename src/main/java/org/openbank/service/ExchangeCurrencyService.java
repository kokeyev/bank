package org.openbank.service;

import org.openbank.dao.currency.CurrencyDao;
import org.openbank.model.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Provides currency lookup, exchange calculation, and admin rate maintenance.
 *
 * <p>Rates are stored relative to KZT, so conversion is calculated by moving through the KZT rate
 * of both currencies.</p>
 */
@Service
public class ExchangeCurrencyService {

  private final CurrencyDao currencyDao;
  public ExchangeCurrencyService(CurrencyDao currencyDao) {
    this.currencyDao = currencyDao;
  }

  /**
   * Returns currencies available for exchange and account operations.
   *
   * @return configured currency records
   */
  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  /**
   * Calculates the converted amount between two currencies.
   *
   * @param fromCurrencyId source currency identifier
   * @param toCurrencyId target currency identifier
   * @param amount positive amount in the source currency
   * @return amount converted to the target currency, rounded to two decimals
   * @throws IllegalArgumentException when currencies or amount are missing or invalid
   */
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

  /**
   * Updates the rate of one currency relative to KZT.
   *
   * @param currencyId currency to update
   * @param rateToKzt positive rate relative to KZT
   * @return {@code true} when the DAO updates the rate
   * @throws IllegalArgumentException when the currency id or rate is invalid
   */
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
