package org.openbank.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Provides bank settings service operations.
 */
@Service
public class BankSettingsService {

  private BigDecimal transferFeePercent = BigDecimal.ZERO;

  /**
   * Handles get transfer fee percent.
   */
  public BigDecimal getTransferFeePercent() {
    return transferFeePercent;
  }

  /**
   * Handles set transfer fee percent.
   */
  public void setTransferFeePercent(BigDecimal transferFeePercent) {
    if (transferFeePercent == null || transferFeePercent.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Комиссия не может быть меньше нуля");
    }

    this.transferFeePercent = transferFeePercent;
  }

  /**
   * Handles calculate transfer fee.
   */
  public BigDecimal calculateTransferFee(BigDecimal amount) {
    if (amount == null || transferFeePercent.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    return amount.multiply(transferFeePercent)
        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }
}
