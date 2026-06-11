package org.author.demo.dto;

public class LoanPaymentScheduleItem {

  private final Integer number;
  private final String dueDate;
  private final String payment;
  private final String status;

  public LoanPaymentScheduleItem(Integer number, String dueDate, String payment, String status) {
    this.number = number;
    this.dueDate = dueDate;
    this.payment = payment;
    this.status = status;
  }

  public Integer getNumber() {
    return number;
  }

  public String getDueDate() {
    return dueDate;
  }

  public String getPayment() {
    return payment;
  }

  public String getStatus() {
    return status;
  }
}
