package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class CardTransferRequest {

  @NotNull(message = "{validation.senderAccount.required}")
  private Long senderAccountId;

  @NotBlank(message = "{validation.cardNumber.required}")
  @Pattern(regexp = "\\d{16}", message = "{validation.cardNumber.format}")
  private String receiverCardNumber;

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
