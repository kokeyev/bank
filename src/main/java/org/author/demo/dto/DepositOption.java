package org.author.demo.dto;

public class DepositOption {

  private final Long depositId;
  private final String label;

  public DepositOption(Long depositId, String label) {
    this.depositId = depositId;
    this.label = label;
  }

  public Long getDepositId() {
    return depositId;
  }

  public String getLabel() {
    return label;
  }
}
