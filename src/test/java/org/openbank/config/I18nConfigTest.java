package org.openbank.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nConfigTest {

  private final MessageSource messageSource = new AppConfig().messageSource();

  @Test
  void messageSourceResolvesRussianEnglishAndKazakhBundles() {
    assertEquals("Настройки", messageSource.getMessage("settings.title", null, Locale.forLanguageTag("ru")));
    assertEquals("Settings", messageSource.getMessage("settings.title", null, Locale.ENGLISH));
    assertEquals("Баптаулар", messageSource.getMessage("settings.title", null, Locale.forLanguageTag("kk")));
  }

  @Test
  void bankingPagesHaveLocalizedLabels() {
    assertEquals("Перевод между своими счетами", messageSource.getMessage("transfers.between", null, Locale.forLanguageTag("ru")));
    assertEquals("Transfer between own accounts", messageSource.getMessage("transfers.between", null, Locale.ENGLISH));
    assertEquals("Өз шоттары арасында аудару", messageSource.getMessage("transfers.between", null, Locale.forLanguageTag("kk")));
  }
}
