package org.openbank.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public class PhoneTransferRequest {

  @NotNull(message = "{validation.senderAccount.required}")
  private Long senderAccountId;

  @NotBlank(message = "{validation.phone.required}")
  @Pattern(regexp = "^\\+?\\d[\\d\\s()-]{8,20}$", message = "{validation.phone.format}")
  private String receiverPhoneNumber;

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
