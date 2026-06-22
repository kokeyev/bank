package org.openbank.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class I18nConfigTest {

  private static final String SETTINGS_TITLE_CODE = "settings.title";
  private static final String TRANSFERS_BETWEEN_CODE = "transfers.between";
  private static final Locale RUSSIAN_LOCALE = Locale.forLanguageTag("ru");
  private static final Locale KAZAKH_LOCALE = Locale.forLanguageTag("kk");
  private static final String SETTINGS_TITLE_RU = "Настройки";
  private static final String SETTINGS_TITLE_EN = "Settings";
  private static final String SETTINGS_TITLE_KK = "Баптаулар";
  private static final String TRANSFERS_BETWEEN_RU = "Перевод между своими счетами";
  private static final String TRANSFERS_BETWEEN_EN = "Transfer between own accounts";
  private static final String TRANSFERS_BETWEEN_KK = "Өз шоттары арасында аудару";

  private final MessageSource messageSource = new AppConfig().messageSource();

  @Test
  void messageSourceResolvesRussianEnglishAndKazakhBundles() {
    assertEquals(SETTINGS_TITLE_RU, messageSource.getMessage(SETTINGS_TITLE_CODE, null, RUSSIAN_LOCALE));
    assertEquals(SETTINGS_TITLE_EN, messageSource.getMessage(SETTINGS_TITLE_CODE, null, Locale.ENGLISH));
    assertEquals(SETTINGS_TITLE_KK, messageSource.getMessage(SETTINGS_TITLE_CODE, null, KAZAKH_LOCALE));
  }

  @Test
  void bankingPagesHaveLocalizedLabels() {
    assertEquals(TRANSFERS_BETWEEN_RU, messageSource.getMessage(TRANSFERS_BETWEEN_CODE, null, RUSSIAN_LOCALE));
    assertEquals(TRANSFERS_BETWEEN_EN, messageSource.getMessage(TRANSFERS_BETWEEN_CODE, null, Locale.ENGLISH));
    assertEquals(TRANSFERS_BETWEEN_KK, messageSource.getMessage(TRANSFERS_BETWEEN_CODE, null, KAZAKH_LOCALE));
  }
}
