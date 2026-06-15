package org.openbank.dto;

public class LoanOption {

  private final Long loanId;
  private final String label;

  public LoanOption(Long loanId, String label) {
    this.loanId = loanId;
    this.label = label;
  }

  public Long getLoanId() {
    return loanId;
  }

  public String getLabel() {
    return label;
  }
}
