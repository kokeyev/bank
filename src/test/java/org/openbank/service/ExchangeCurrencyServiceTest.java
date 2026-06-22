package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.CurrencyDao;
import org.openbank.service.impl.ExchangeCurrencyServiceImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExchangeCurrencyServiceTest {

  private static final Long FROM_CURRENCY_ID = 1L;
  private static final Long TO_CURRENCY_ID = 2L;
  private static final BigDecimal FROM_RATE_TO_KZT = new BigDecimal("500");
  private static final BigDecimal TO_RATE_TO_KZT = new BigDecimal("250");
  private static final BigDecimal AMOUNT_TO_CONVERT = new BigDecimal("10");
  private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("20.00");
  private static final BigDecimal SAME_CURRENCY_AMOUNT = new BigDecimal("42");
  private static final BigDecimal UPDATED_RATE_TO_KZT = new BigDecimal("510");

  @Mock
  private CurrencyDao currencyDao;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private ExchangeCurrencyServiceImpl service;

  @Test
  void calculateConvertsThroughKztRates() {
    when(currencyDao.getCurrencyRateToKztById(FROM_CURRENCY_ID)).thenReturn(FROM_RATE_TO_KZT);
    when(currencyDao.getCurrencyRateToKztById(TO_CURRENCY_ID)).thenReturn(TO_RATE_TO_KZT);

    BigDecimal result = service.calculate(FROM_CURRENCY_ID, TO_CURRENCY_ID, AMOUNT_TO_CONVERT);

    assertEquals(CONVERTED_AMOUNT, result);
  }

  @Test
  void calculateReturnsAmountForSameCurrency() {
    assertEquals(SAME_CURRENCY_AMOUNT, service.calculate(FROM_CURRENCY_ID, FROM_CURRENCY_ID, SAME_CURRENCY_AMOUNT));
  }

  @Test
  void calculateRejectsMissingCurrency() {
    assertThrows(IllegalArgumentException.class, () -> service.calculate(null, FROM_CURRENCY_ID, BigDecimal.TEN));
  }

  @Test
  void updateRateValidatesAndDelegates() {
    when(currencyDao.updateCurrencyRate(FROM_CURRENCY_ID, UPDATED_RATE_TO_KZT)).thenReturn(true);

    boolean updated = service.updateRate(FROM_CURRENCY_ID, UPDATED_RATE_TO_KZT);

    assertEquals(true, updated);
    verify(currencyDao).updateCurrencyRate(FROM_CURRENCY_ID, UPDATED_RATE_TO_KZT);
  }
}
