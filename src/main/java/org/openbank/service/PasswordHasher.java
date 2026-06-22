package org.openbank.service;

/**
 * Defines password hashing and verification operations.
 */
public interface PasswordHasher {

  /** Hashes a raw password with a fresh salt. */
  String hash(String password);

  /** Compares a raw password with a stored salted hash. */
  boolean matches(String password, String storedHash);
}
