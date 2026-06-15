package org.openbank.exception;

public class BankDataAccessException extends RuntimeException {

  public BankDataAccessException(String message) {
    super(message);
  }

  public BankDataAccessException(String message, Throwable cause) {
    super(message, cause);
  }
}
