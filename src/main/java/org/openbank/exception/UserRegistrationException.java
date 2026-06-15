package org.openbank.exception;

import java.util.List;

public class UserRegistrationException extends RuntimeException {

  private final List<String> errors;

  public UserRegistrationException(List<String> errors) {
    super(String.join(" ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> getErrors() {
    return errors;
  }
}
