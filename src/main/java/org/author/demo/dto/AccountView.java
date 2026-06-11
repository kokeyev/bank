package org.author.demo.dto;

public class AccountView {

  private final String name;
  private final String balance;
  private final String currency;
  private final String cardNumber;
  private final String expiryDate;
  private final String cvv;
  private final String transactionLimit;
  private final String transactionLimitValue;
  private final Long accountId;
  private final String status;
  private final String statusLabel;
  private final boolean main;
  private final boolean active;

  public AccountView(Long accountId, String name, String balance, String currency, String cardNumber, String expiryDate, String cvv, String transactionLimit, String transactionLimitValue, String status, String statusLabel, boolean main, boolean active) {
    this.accountId = accountId;
    this.name = name;
    this.balance = balance;
    this.currency = currency;
    this.cardNumber = cardNumber;
    this.expiryDate = expiryDate;
    this.cvv = cvv;
    this.transactionLimit = transactionLimit;
    this.transactionLimitValue = transactionLimitValue;
    this.status = status;
    this.statusLabel = statusLabel;
    this.main = main;
    this.active = active;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getName() {
    return name;
  }

  public String getBalance() {
    return balance;
  }

  public String getCurrency() {
    return currency;
  }

  public String getCardNumber() {
    return cardNumber;
  }

  public String getExpiryDate() {
    return expiryDate;
  }

  public String getCvv() {
    return cvv;
  }

  public String getTransactionLimit() {
    return transactionLimit;
  }

  public String getTransactionLimitValue() {
    return transactionLimitValue;
  }

  public String getStatus() {
    return status;
  }

  public String getStatusLabel() {
    return statusLabel;
  }

  public boolean isMain() {
    return main;
  }

  public boolean isActive() {
    return active;
  }
}
