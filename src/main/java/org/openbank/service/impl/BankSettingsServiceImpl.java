package org.openbank.service.impl;

import org.openbank.service.BankSettingsService;
import org.openbank.service.Messages;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BankSettingsServiceImpl implements BankSettingsService {

  private BigDecimal transferFeePercent = BigDecimal.ZERO;

  public BigDecimal getTransferFeePercent() {
    return transferFeePercent;
  }

  public void setTransferFeePercent(BigDecimal transferFeePercent) {
    if (transferFeePercent == null || transferFeePercent.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(Messages.get("admin.validation.fee.negative"));
    }

    this.transferFeePercent = transferFeePercent;
  }

  public BigDecimal calculateTransferFee(BigDecimal amount) {
    if (amount == null || transferFeePercent.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }

    return amount.multiply(transferFeePercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
  }
}
