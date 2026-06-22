package org.openbank.service;

/**
 * Defines localized message lookup operations.
 */
public interface MessageService {

  /** Resolves a message code with optional arguments. */
  String get(String code, Object... args);
}
