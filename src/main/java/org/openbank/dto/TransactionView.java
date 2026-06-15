package org.openbank.dto;

public class TransactionView {

  private final String date;
  private final String type;
  private final String amount;
  private final String fee;
  private final String message;

  public TransactionView(String date, String type, String amount, String fee, String message) {
    this.date = date;
    this.type = type;
    this.amount = amount;
    this.fee = fee;
    this.message = message;
  }

  public String getDate() {
    return date;
  }

  public String getType() {
    return type;
  }

  public String getAmount() {
    return amount;
  }

  public String getFee() {
    return fee;
  }

  public String getMessage() {
    return message;
  }
}
