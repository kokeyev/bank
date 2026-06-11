package org.author.demo.services;

import java.util.List;

public class ContactUpdateException extends RuntimeException {

  private final List<String> errors;

  public ContactUpdateException(List<String> errors) {
    super(String.join(" ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> getErrors() {
    return errors;
  }
}
