package org.openbank.service;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates and verifies salted password hashes for users and staff.
 *
 * <p>The stored format is {@code base64(salt):base64(hash)}. This keeps raw passwords and demo
 * admin credentials out of code and seed scripts.</p>
 */
@Component
public class PasswordHasher {

  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Hashes a raw password with a fresh random salt.
   *
   * @param password raw password from a registration or password-change form
   * @return encoded salt and hash suitable for storing in the users table
   * @throws IllegalStateException when the hashing algorithm is unavailable
   */
  public String hash(String password) {
    byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);
    byte[] hash = sha256(password, salt);

    return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
  }

  /**
   * Compares a raw password with a stored salted hash.
   *
   * @param password raw password supplied during login
   * @param storedHash database value in {@code salt:hash} format
   * @return {@code true} when the password matches the stored hash
   */
  public boolean matches(String password, String storedHash) {
    String[] parts = storedHash.split(":");
    if (parts.length != 2) {
      return false;
    }

    byte[] salt = Base64.getDecoder().decode(parts[0]);
    byte[] expectedHash = Base64.getDecoder().decode(parts[1]);
    byte[] actualHash = sha256(password, salt);

    return MessageDigest.isEqual(expectedHash, actualHash);
  }

  private byte[] sha256(String password, byte[] salt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(salt);
      return digest.digest(password.getBytes());
    } catch (Exception e) {
      throw new IllegalStateException(Messages.get("password.error.processing"), e);
    }
  }
}
