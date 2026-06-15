package org.openbank.dao.currency;

import org.openbank.model.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Defines the currency dao contract.
 */
public interface CurrencyDao {

  /** Finds a currency by id. */
  Optional<Currency> getCurrencyById(Long currencyId);

  /** Finds a currency by name. */
  Optional<Currency> getCurrencyByName(String name);

  /** Returns all currencies. */
  List<Currency> getAllCurrencies();

  /** Returns a currency name by id. */
  String getCurrencyNameById(Long currencyId);

  /** Returns a currency rate to KZT. */
  BigDecimal getCurrencyRateToKztById(Long currencyId);

  /** Updates a currency rate to KZT. */
  boolean updateCurrencyRate(Long currencyId, BigDecimal rateToKzt);

}
