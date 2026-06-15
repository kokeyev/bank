package org.openbank.exception;

public class BankInfrastructureException extends RuntimeException {

  public BankInfrastructureException(String message, Throwable cause) {
    super(message, cause);
  }
}
