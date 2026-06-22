package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.openbank.service.impl.PasswordHasherImpl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

  private static final String PASSWORD = "strong-password";
  private static final String WRONG_PASSWORD = "wrong-password";
  private static final String MALFORMED_HASH = "plain-text";

  private final PasswordHasher passwordHasher = new PasswordHasherImpl((code, args) -> code);

  @Test
  void hashCreatesSaltedHashThatMatchesOriginalPassword() {
    String firstHash = passwordHasher.hash(PASSWORD);
    String secondHash = passwordHasher.hash(PASSWORD);

    assertNotEquals(firstHash, secondHash);
    assertTrue(passwordHasher.matches(PASSWORD, firstHash));
    assertFalse(passwordHasher.matches(WRONG_PASSWORD, firstHash));
  }

  @Test
  void matchesRejectsMalformedStoredHash() {
    assertFalse(passwordHasher.matches(PASSWORD, MALFORMED_HASH));
  }
}
