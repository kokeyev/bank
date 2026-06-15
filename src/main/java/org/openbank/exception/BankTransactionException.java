package org.openbank.exception;

public class BankTransactionException extends RuntimeException {

  public BankTransactionException(String message, Throwable cause) {
    super(message, cause);
  }
}
