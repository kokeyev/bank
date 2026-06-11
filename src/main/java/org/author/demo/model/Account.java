package org.author.demo.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Account {

  private Long accountId;
  private Long userId;
  private String cardNumber;
  private String cvv;
  private LocalDate expiryDate;
  private BigDecimal balance;
  private Long currencyId;
  private String status;
  private BigDecimal transactionLimit;
  private String name;
  private Boolean main;

  public Account(Long accountId, Long userId, String cardNumber, String cvv, LocalDate expiryDate, BigDecimal balance, Long currencyId, String status, BigDecimal transactionLimit, String name, Boolean main) {
    this.accountId = accountId;
    this.userId = userId;
    this.cardNumber = cardNumber;
    this.cvv = cvv;
    this.expiryDate = expiryDate;
    this.balance = balance;
    this.currencyId = currencyId;
    this.status = status;
    this.transactionLimit = transactionLimit;
    this.name = name;
    this.main = main;
  }

  public Long getAccountId() {
    return accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public Long getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(Long currencyId) {
    this.currencyId = currencyId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getTransactionLimit() {
    return transactionLimit;
  }

  public void setTransactionLimit(BigDecimal transactionLimit) {
    this.transactionLimit = transactionLimit;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Boolean getMain() {
    return main;
  }

  public void setMain(Boolean main) {
    this.main = main;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Account account)) return false;
    return Objects.equals(getAccountId(), account.getAccountId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getAccountId());
  }

  @Override
  public String toString() {
    return "Account{" +
        "accountId=" + accountId +
        ", userId=" + userId +
        ", cardNumber='" + cardNumber + '\'' +
        ", cvv='" + cvv + '\'' +
        ", expiryDate=" + expiryDate +
        ", balance=" + balance +
        ", currencyId=" + currencyId +
        ", status='" + status + '\'' +
        ", transactionLimit=" + transactionLimit +
        ", name='" + name + '\'' +
        ", main=" + main +
        '}';
  }
}
