package org.author.demo.dto;

public class DepositTypeOption {

  private final Long depositTypeId;
  private final String label;

  public DepositTypeOption(Long depositTypeId, String label) {
    this.depositTypeId = depositTypeId;
    this.label = label;
  }

  public Long getDepositTypeId() {
    return depositTypeId;
  }

  public String getLabel() {
    return label;
  }
}
