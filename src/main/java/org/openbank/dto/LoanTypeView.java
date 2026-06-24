package org.openbank.dto;

public class LoanTypeView {

  private final Long loanTypeId;
  private final String name;
  private final String urlPath;
  private final String tag;
  private final String description;
  private final String amountRange;
  private final String duration;
  private final String rate;

  public LoanTypeView(Long loanTypeId, String name, String urlPath, String tag, String description, String amountRange, String duration, String rate) {
    this.loanTypeId = loanTypeId;
    this.name = name;
    this.urlPath = urlPath;
    this.tag = tag;
    this.description = description;
    this.amountRange = amountRange;
    this.duration = duration;
    this.rate = rate;
  }

  public Long getLoanTypeId() {
    return loanTypeId;
  }

  public String getName() {
    return name;
  }

  public String getUrlPath() {
    return urlPath;
  }

  public String getTag() {
    return tag;
  }

  public String getDescription() {
    return description;
  }

  public String getAmountRange() {
    return amountRange;
  }

  public String getDuration() {
    return duration;
  }

  public String getRate() {
    return rate;
  }
}
