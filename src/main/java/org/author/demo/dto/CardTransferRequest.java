package org.author.demo.dto;

import java.math.BigDecimal;

public class CardTransferRequest {

  private Long senderAccountId;
  private String receiverCardNumber;
  private BigDecimal amount;
  private String message;

  public Long getSenderAccountId() {
    return senderAccountId;
  }

  public void setSenderAccountId(Long senderAccountId) {
    this.senderAccountId = senderAccountId;
  }

  public String getReceiverCardNumber() {
    return receiverCardNumber;
  }

  public void setReceiverCardNumber(String receiverCardNumber) {
    this.receiverCardNumber = receiverCardNumber;
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
