package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransferBetweenAccountsRequest {

  @NotNull(message = "{validation.senderAccount.required}")
  private Long senderAccountId;

  @NotNull(message = "{validation.receiverAccount.required}")
  private Long receiverAccountId;

  @NotNull(message = "{validation.amount.required}")
  @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
  private BigDecimal amount;
  private String message;

  public Long getSenderAccountId() {
    return senderAccountId;
  }

  public void setSenderAccountId(Long senderAccountId) {
    this.senderAccountId = senderAccountId;
  }

  public Long getReceiverAccountId() {
    return receiverAccountId;
  }

  public void setReceiverAccountId(Long receiverAccountId) {
    this.receiverAccountId = receiverAccountId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
