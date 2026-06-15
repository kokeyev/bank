package org.openbank.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankSettingsServiceTest {

  private final BankSettingsService service = new BankSettingsService();

  @Test
  void calculateTransferFeeUsesConfiguredPercent() {
    service.setTransferFeePercent(new BigDecimal("1.5"));

    assertEquals(new BigDecimal("15.00"), service.calculateTransferFee(new BigDecimal("1000")));
  }

  @Test
  void calculateTransferFeeReturnsZeroWhenAmountIsNull() {
    service.setTransferFeePercent(new BigDecimal("2"));

    assertEquals(BigDecimal.ZERO, service.calculateTransferFee(null));
  }

  @Test
  void setTransferFeePercentRejectsNegativeValue() {
    assertThrows(IllegalArgumentException.class, () -> service.setTransferFeePercent(new BigDecimal("-0.01")));
  }
}
