package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.openbank.service.impl.BankSettingsServiceImpl;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BankSettingsServiceTest {

  private static final BigDecimal TRANSFER_FEE_PERCENT = new BigDecimal("1.5");
  private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal EXPECTED_TRANSFER_FEE = new BigDecimal("15.00");
  private static final BigDecimal SECOND_TRANSFER_FEE_PERCENT = new BigDecimal("2");
  private static final BigDecimal NEGATIVE_TRANSFER_FEE_PERCENT = new BigDecimal("-0.01");

  private final BankSettingsService service = new BankSettingsServiceImpl((code, args) -> code);

  @Test
  void calculateTransferFeeUsesConfiguredPercent() {
    service.setTransferFeePercent(TRANSFER_FEE_PERCENT);

    assertEquals(EXPECTED_TRANSFER_FEE, service.calculateTransferFee(TRANSFER_AMOUNT));
  }

  @Test
  void calculateTransferFeeReturnsZeroWhenAmountIsNull() {
    service.setTransferFeePercent(SECOND_TRANSFER_FEE_PERCENT);

    assertEquals(BigDecimal.ZERO, service.calculateTransferFee(null));
  }

  @Test
  void setTransferFeePercentRejectsNegativeValue() {
    Executable executable = () -> service.setTransferFeePercent(NEGATIVE_TRANSFER_FEE_PERCENT);
    assertThrows(IllegalArgumentException.class, executable);
  }
}
