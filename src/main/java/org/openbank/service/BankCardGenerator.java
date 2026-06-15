package org.openbank.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Provides bank card generator operations.
 */
@Component
public class BankCardGenerator {

  private static final int CARD_NUMBER_LENGTH = 16;
  private static final int DEFAULT_VALID_YEARS = 4;

  private final SecureRandom random = new SecureRandom();

  /**
   * Handles generate card number.
   */
  public String generateCardNumber() {
    StringBuilder cardNumber = new StringBuilder(CARD_NUMBER_LENGTH);
    cardNumber.append('4');

    while (cardNumber.length() < CARD_NUMBER_LENGTH - 1) {
      cardNumber.append(random.nextInt(10));
    }

    cardNumber.append(calculateCheckDigit(cardNumber.toString()));
    return cardNumber.toString();
  }

  /**
   * Handles generate expiry date.
   */
  public LocalDate generateExpiryDate() {
    return LocalDate.now()
        .plusYears(DEFAULT_VALID_YEARS)
        .withDayOfMonth(1);
  }

  /**
   * Handles generate cvv.
   */
  public String generateCvv() {
    return String.valueOf(random.nextInt(900) + 100);
  }

  /**
   * Handles is valid card number.
   */
  public boolean isValidCardNumber(String cardNumber) {
    if (cardNumber == null || !cardNumber.matches("\\d{16}")) {
      return false;
    }

    int sum = 0;
    boolean doubleDigit = false;

    for (int i = cardNumber.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(cardNumber.charAt(i));

      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      doubleDigit = !doubleDigit;
    }

    return sum % 10 == 0;
  }

  private int calculateCheckDigit(String cardNumberWithoutCheckDigit) {
    int sum = 0;
    boolean doubleDigit = true;

    for (int i = cardNumberWithoutCheckDigit.length() - 1; i >= 0; i--) {
      int digit = Character.getNumericValue(cardNumberWithoutCheckDigit.charAt(i));

      if (doubleDigit) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      doubleDigit = !doubleDigit;
    }

    return (10 - (sum % 10)) % 10;
  }
}
