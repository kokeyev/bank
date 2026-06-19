package org.openbank.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stores runtime banking settings that affect financial calculations.
 *
 * <p>The current implementation keeps the transfer fee in application memory because the setting is
 * changed from the admin panel during one application run.</p>
 */
@Service
public class BankSettingsService {

  private BigDecimal transferFeePercent = BigDecimal.ZERO;

  /**
   * Returns the current transfer fee percentage.
   *
   * @return non-negative percent value, for example {@code 1.5}
   */
  public BigDecimal getTransferFeePercent() {
    return transferFeePercent;
  }

  /**
   * Updates the transfer fee percentage used by new transfer operations.
   *
   * @param transferFeePercent non-negative fee percent
   * @throws IllegalArgumentException when the value is {@code null} or negative
   */
  public void setTransferFeePercent(BigDecimal transferFeePercent) {
    if (transferFeePercent == null || transferFeePercent.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(Messages.get("admin.validation.fee.negative"));
    }

    this.transferFeePercent = transferFeePercent;
  }

  /**
   * Calculates the fee for a transfer amount using the current percentage.
   *
   * @param amount transfer amount before fee
   * @return rounded fee amount, or zero when the amount is missing or the fee is disabled
   */
  public BigDecimal calculateTransferFee(BigDecimal amount) {
    if (amount == null || transferFeePercent.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    return amount.multiply(transferFeePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }
}
