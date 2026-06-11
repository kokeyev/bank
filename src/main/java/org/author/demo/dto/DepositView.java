package org.author.demo.dto;

public class DepositView {

  private final Long depositId;
  private final String name;
  private final String amount;
  private final String amountValue;
  private final String currency;
  private final String rate;
  private final String duration;
  private final String startDate;
  private final String status;
  private final String autoRenewal;
  private final String reinvestInterest;
  private final boolean active;
  private final boolean withdrawalAllowed;

  public DepositView(Long depositId, String name, String amount, String amountValue, String currency, String rate, String duration, String startDate, String status, String autoRenewal, String reinvestInterest, boolean active, boolean withdrawalAllowed) {
    this.depositId = depositId;
    this.name = name;
    this.amount = amount;
    this.amountValue = amountValue;
    this.currency = currency;
    this.rate = rate;
    this.duration = duration;
    this.startDate = startDate;
    this.status = status;
    this.autoRenewal = autoRenewal;
    this.reinvestInterest = reinvestInterest;
    this.active = active;
    this.withdrawalAllowed = withdrawalAllowed;
  }

  public Long getDepositId() {
    return depositId;
  }

  public String getName() {
    return name;
  }

  public String getAmount() {
    return amount;
  }

  public String getAmountValue() {
    return amountValue;
  }

  public String getCurrency() {
    return currency;
  }

  public String getRate() {
    return rate;
  }

  public String getDuration() {
    return duration;
  }

  public String getStartDate() {
    return startDate;
  }

  public String getStatus() {
    return status;
  }

  public String getAutoRenewal() {
    return autoRenewal;
  }

  public String getReinvestInterest() {
    return reinvestInterest;
  }

  public boolean isActive() {
    return active;
  }

  public boolean isWithdrawalAllowed() {
    return withdrawalAllowed;
  }
}
