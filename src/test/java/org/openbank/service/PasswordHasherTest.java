package org.openbank.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

  private final PasswordHasher passwordHasher = new PasswordHasher();

  @Test
  void hashCreatesSaltedHashThatMatchesOriginalPassword() {
    String firstHash = passwordHasher.hash("strong-password");
    String secondHash = passwordHasher.hash("strong-password");

    assertNotEquals(firstHash, secondHash);
    assertTrue(passwordHasher.matches("strong-password", firstHash));
    assertFalse(passwordHasher.matches("wrong-password", firstHash));
  }

  @Test
  void matchesRejectsMalformedStoredHash() {
    assertFalse(passwordHasher.matches("password", "plain-text"));
  }
}
