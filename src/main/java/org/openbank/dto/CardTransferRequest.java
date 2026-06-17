package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class CardTransferRequest {

  @NotNull(message = "Sender account is required")
  private Long senderAccountId;

  @NotBlank(message = "Card number is required")
  @Pattern(regexp = "\\d{16}", message = "Card number must contain exactly 16 digits")
  private String receiverCardNumber;

  @NotNull(message = "Amount is required")
  @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
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
