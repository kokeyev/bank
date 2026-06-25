package org.openbank.service.impl;

import org.openbank.service.ExchangeCurrencyService;
import org.openbank.service.MessageService;
import org.openbank.dao.CurrencyDao;
import org.openbank.model.Currency;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ExchangeCurrencyServiceImpl implements ExchangeCurrencyService {

  private final CurrencyDao currencyDao;
  private final MessageService messageService;

  public ExchangeCurrencyServiceImpl(CurrencyDao currencyDao, MessageService messageService) {
    this.currencyDao = currencyDao;
    this.messageService = messageService;
  }

  public List<Currency> getAllCurrencies() {
    return currencyDao.getAllCurrencies();
  }

  public BigDecimal calculate(Long fromCurrencyId, Long toCurrencyId, BigDecimal amount) {
    if (fromCurrencyId == null || toCurrencyId == null) {
      throw new IllegalArgumentException(messageService.get("exchange.validation.currencies.required"));
    }
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(messageService.get("validation.amount.positive"));
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
      throw new IllegalArgumentException(messageService.get("validation.currency.required"));
    }
    if (rateToKzt == null || rateToKzt.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(messageService.get("exchange.validation.rate.positive"));
    }

    return currencyDao.updateCurrencyRate(currencyId, rateToKzt);
  }
}
