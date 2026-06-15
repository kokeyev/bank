package org.openbank.service;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Provides password hasher operations.
 */
@Component
public class PasswordHasher {

  private static final String HASH_PREFIX = "pbkdf2_sha256";
  private static final int PASSWORD_HASH_ITERATIONS = 120_000;
  private static final int PASSWORD_KEY_LENGTH = 256;
  private static final int SALT_BYTES = 16;

  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Handles hash.
   */
  public String hash(String password) {
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);

    try {
      byte[] hash = generateHash(password, salt, PASSWORD_HASH_ITERATIONS, PASSWORD_KEY_LENGTH);
      return HASH_PREFIX
          + "$"
          + PASSWORD_HASH_ITERATIONS
          + "$"
          + Base64.getEncoder().encodeToString(salt)
          + "$"
          + Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Не удалось обработать пароль", e);
    }
  }

  /**
   * Handles matches.
   */
  public boolean matches(String password, String storedPasswordHash) {
    if (storedPasswordHash == null || !storedPasswordHash.startsWith(HASH_PREFIX + "$")) {
      return false;
    }

    String[] parts = storedPasswordHash.split("\\$");
    if (parts.length != 4) {
      return false;
    }

    try {
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
      byte[] actualHash = generateHash(password, salt, iterations, expectedHash.length * 8);

      return MessageDigest.isEqual(expectedHash, actualHash);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      return false;
    }
  }

  private byte[] generateHash(String password, byte[] salt, int iterations, int keyLength)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLength);
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
    return factory.generateSecret(spec).getEncoded();
  }
}
