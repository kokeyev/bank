package org.openbank.dto;

import java.util.List;

public class LoanView {

  private final Long loanId;
  private final String typeName;
  private final String amount;
  private final String rate;
  private final String duration;
  private final String monthlyPayment;
  private final String status;
  private final String startDate;
  private final boolean offered;
  private final boolean active;
  private final String penalty;
  private final List<LoanPaymentScheduleItem> schedule;

  public LoanView(Long loanId, String typeName, String amount, String rate, String duration, String monthlyPayment, String status, String startDate, boolean offered) {
    this(loanId, typeName, amount, rate, duration, monthlyPayment, status, startDate, offered, false, "0.00 ₸", List.of());
  }

  public LoanView(Long loanId, String typeName, String amount, String rate, String duration, String monthlyPayment, String status, String startDate, boolean offered, boolean active, String penalty, List<LoanPaymentScheduleItem> schedule) {
    this.loanId = loanId;
    this.typeName = typeName;
    this.amount = amount;
    this.rate = rate;
    this.duration = duration;
    this.monthlyPayment = monthlyPayment;
    this.status = status;
    this.startDate = startDate;
    this.offered = offered;
    this.active = active;
    this.penalty = penalty;
    this.schedule = schedule;
  }

  public Long getLoanId() {
    return loanId;
  }

  public String getTypeName() {
    return typeName;
  }

  public String getAmount() {
    return amount;
  }

  public String getRate() {
    return rate;
  }

  public String getDuration() {
    return duration;
  }

  public String getMonthlyPayment() {
    return monthlyPayment;
  }

  public String getStatus() {
    return status;
  }

  public String getStartDate() {
    return startDate;
  }

  public boolean isOffered() {
    return offered;
  }

  public boolean isActive() {
    return active;
  }

  public String getPenalty() {
    return penalty;
  }

  public List<LoanPaymentScheduleItem> getSchedule() {
    return schedule;
  }
}
