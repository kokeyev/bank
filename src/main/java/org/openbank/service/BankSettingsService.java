package org.openbank.service;

import java.math.BigDecimal;

/**
 * Defines runtime banking settings operations.
 */
public interface BankSettingsService {

  /** Returns the current transfer fee percentage. */
  BigDecimal getTransferFeePercent();

  /** Updates the transfer fee percentage. */
  void setTransferFeePercent(BigDecimal transferFeePercent);

  /** Calculates the transfer fee for an amount. */
  BigDecimal calculateTransferFee(BigDecimal amount);
}
