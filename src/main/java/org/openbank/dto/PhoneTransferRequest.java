package org.openbank.dto;

import java.math.BigDecimal;

public class PhoneTransferRequest {

  private Long senderAccountId;
  private String receiverPhoneNumber;
  private BigDecimal amount;
  private String message;

  public Long getSenderAccountId() {
    return senderAccountId;
  }

  public void setSenderAccountId(Long senderAccountId) {
    this.senderAccountId = senderAccountId;
  }

  public String getReceiverPhoneNumber() {
    return receiverPhoneNumber;
  }

  public void setReceiverPhoneNumber(String receiverPhoneNumber) {
    this.receiverPhoneNumber = receiverPhoneNumber;
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
