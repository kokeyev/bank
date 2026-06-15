package org.openbank.dto;

import java.math.BigDecimal;

public class TransferBetweenAccountsRequest {

  private Long senderAccountId;
  private Long receiverAccountId;
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
