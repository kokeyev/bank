package org.author.demo.dto;

public class TransferAccountOption {

  private final Long accountId;
  private final String label;

  public TransferAccountOption(Long accountId, String label) {
    this.accountId = accountId;
    this.label = label;
  }

  public Long getAccountId() {
    return accountId;
  }

  public String getLabel() {
    return label;
  }
}
