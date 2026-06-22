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

  @Mock
  private CurrencyDao currencyDao;

  @InjectMocks
  private ExchangeCurrencyServiceImpl service;

  @Test
  void calculateConvertsThroughKztRates() {
    when(currencyDao.getCurrencyRateToKztById(1L)).thenReturn(new BigDecimal("500"));
    when(currencyDao.getCurrencyRateToKztById(2L)).thenReturn(new BigDecimal("250"));

    BigDecimal result = service.calculate(1L, 2L, new BigDecimal("10"));

    assertEquals(new BigDecimal("20.00"), result);
  }

  @Test
  void calculateReturnsAmountForSameCurrency() {
    assertEquals(new BigDecimal("42"), service.calculate(1L, 1L, new BigDecimal("42")));
  }

  @Test
  void calculateRejectsMissingCurrency() {
    assertThrows(IllegalArgumentException.class, () -> service.calculate(null, 1L, BigDecimal.TEN));
  }

  @Test
  void updateRateValidatesAndDelegates() {
    when(currencyDao.updateCurrencyRate(1L, new BigDecimal("510"))).thenReturn(true);

    boolean updated = service.updateRate(1L, new BigDecimal("510"));

    assertEquals(true, updated);
    verify(currencyDao).updateCurrencyRate(1L, new BigDecimal("510"));
  }
}
