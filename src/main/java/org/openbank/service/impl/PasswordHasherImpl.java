package org.openbank.service.impl;

import org.openbank.service.PasswordHasher;
import org.openbank.service.Messages;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasherImpl implements PasswordHasher {

  private final SecureRandom secureRandom = new SecureRandom();

  public String hash(String password) {
    byte[] salt = new byte[16];
    secureRandom.nextBytes(salt);
    byte[] hash = sha256(password, salt);

    return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
  }

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
