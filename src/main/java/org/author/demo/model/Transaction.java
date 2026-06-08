package org.author.demo.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Transaction {

  private Long transactionId;
  private Long senderAccountId;
  private Long receiverAccountId;
  private LocalDateTime transactionDate;
  private BigDecimal amount;
  private Long currencyId;
  private BigDecimal fee;
  private String message;
  private String transactionType;

  public Transaction(Long transactionId, Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType) {
    this.transactionId = transactionId;
    this.senderAccountId = senderAccountId;
    this.receiverAccountId = receiverAccountId;
    this.transactionDate = transactionDate;
    this.amount = amount;
    this.currencyId = currencyId;
    this.fee = fee;
    this.message = message;
    this.transactionType = transactionType;
  }

  public Long getTransactionId() {
    return transactionId;
  }

  public void setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
  }

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

  public LocalDateTime getTransactionDate() {
    return transactionDate;
  }

  public void setTransactionDate(LocalDateTime transactionDate) {
    this.transactionDate = transactionDate;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  public BigDecimal getFee() {
    return fee;
  }

  public void setFee(BigDecimal fee) {
    this.fee = fee;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getTransactionType() {
    return transactionType;
  }

  public void setTransactionType(String transactionType) {
    this.transactionType = transactionType;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Transaction that)) return false;
    return Objects.equals(getTransactionId(), that.getTransactionId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getTransactionId());
  }

  @Override
  public String toString() {
    return "Transaction{" +
        "transactionId=" + transactionId +
        ", senderAccountId=" + senderAccountId +
        ", receiverAccountId=" + receiverAccountId +
        ", transactionDate=" + transactionDate +
        ", amount=" + amount +
        ", currencyId=" + currencyId +
        ", fee=" + fee +
        ", message='" + message + '\'' +
        ", transactionType='" + transactionType + '\'' +
        '}';
  }
}
