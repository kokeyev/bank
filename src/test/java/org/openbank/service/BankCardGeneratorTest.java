package org.openbank.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankCardGeneratorTest {

  private final BankCardGenerator generator = new BankCardGenerator();

  @Test
  void generatedCardNumberHasSixteenDigitsAndPassesCheck() {
    String cardNumber = generator.generateCardNumber();

    assertTrue(cardNumber.matches("\\d{16}"));
    assertTrue(cardNumber.startsWith("4"));
    assertTrue(generator.isValidCardNumber(cardNumber));
  }

  @Test
  void validatesOnlyCorrectLuhnCardNumbers() {
    assertTrue(generator.isValidCardNumber("4000000000000002"));
    assertFalse(generator.isValidCardNumber("4000000000000003"));
    assertFalse(generator.isValidCardNumber("4000 0000 0000 0002"));
    assertFalse(generator.isValidCardNumber(null));
  }

  @Test
  void generatedCvvAndExpiryDateHaveExpectedFormat() {
    String cvv = generator.generateCvv();
    LocalDate expiryDate = generator.generateExpiryDate();

    assertTrue(cvv.matches("\\d{3}"));
    assertEquals(1, expiryDate.getDayOfMonth());
    assertTrue(expiryDate.isAfter(LocalDate.now().plusYears(3)));
  }
}
