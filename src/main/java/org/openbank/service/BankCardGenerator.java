package org.openbank.service;

import java.time.LocalDate;

/**
 * Defines bank card value generation and validation.
 */
public interface BankCardGenerator {

  /** Generates a 16-digit card number with a valid checksum. */
  String generateCardNumber();

  /** Generates the default card expiry date. */
  LocalDate generateExpiryDate();

  /** Generates a three-digit CVV value. */
  String generateCvv();

  /** Checks card number format and checksum. */
  boolean isValidCardNumber(String cardNumber);
}
