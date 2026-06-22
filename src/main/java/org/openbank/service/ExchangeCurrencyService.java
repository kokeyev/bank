package org.openbank.service;

import org.openbank.model.Currency;

import java.math.BigDecimal;
import java.util.List;

/**
 * Defines currency lookup, conversion, and rate maintenance.
 */
public interface ExchangeCurrencyService {

  /** Returns currencies available for exchange. */
  List<Currency> getAllCurrencies();

  /** Calculates a converted amount between currencies. */
  BigDecimal calculate(Long fromCurrencyId, Long toCurrencyId, BigDecimal amount);

  /** Updates a currency rate relative to KZT. */
  boolean updateRate(Long currencyId, BigDecimal rateToKzt);
}
