package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.openbank.service.impl.BankCardGeneratorImpl;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankCardGeneratorTest {

  private static final String CARD_NUMBER_PATTERN = "\\d{16}";
  private static final String CARD_NUMBER_PREFIX = "4";
  private static final String VALID_CARD_NUMBER = "4000000000000002";
  private static final String INVALID_CHECKSUM_CARD_NUMBER = "4000000000000003";
  private static final String FORMATTED_CARD_NUMBER = "4000 0000 0000 0002";
  private static final String CVV_PATTERN = "\\d{3}";
  private static final int FIRST_DAY_OF_MONTH = 1;
  private static final int MIN_VALID_YEARS = 3;

  private final BankCardGenerator generator = new BankCardGeneratorImpl();

  @Test
  void generatedCardNumberHasSixteenDigitsAndPassesCheck() {
    String cardNumber = generator.generateCardNumber();

    assertTrue(cardNumber.matches(CARD_NUMBER_PATTERN));
    assertTrue(cardNumber.startsWith(CARD_NUMBER_PREFIX));
    assertTrue(generator.isValidCardNumber(cardNumber));
  }

  @Test
  void validatesOnlyCorrectLuhnCardNumbers() {
    assertTrue(generator.isValidCardNumber(VALID_CARD_NUMBER));
    assertFalse(generator.isValidCardNumber(INVALID_CHECKSUM_CARD_NUMBER));
    assertFalse(generator.isValidCardNumber(FORMATTED_CARD_NUMBER));
    assertFalse(generator.isValidCardNumber(null));
  }

  @Test
  void generatedCvvAndExpiryDateHaveExpectedFormat() {
    String cvv = generator.generateCvv();
    LocalDate expiryDate = generator.generateExpiryDate();

    assertTrue(cvv.matches(CVV_PATTERN));
    assertEquals(FIRST_DAY_OF_MONTH, expiryDate.getDayOfMonth());
    assertTrue(expiryDate.isAfter(LocalDate.now().plusYears(MIN_VALID_YEARS)));
  }
}
