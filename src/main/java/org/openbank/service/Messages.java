package org.openbank.service;

import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class Messages {

  private Messages() {
  }

  public static String get(String code, Object... args) {
    try {
      ResourceBundle bundle = ResourceBundle.getBundle("messages", LocaleContextHolder.getLocale());
      String pattern = bundle.getString(code);

      return args == null || args.length == 0 ? pattern : MessageFormat.format(pattern, args);
    } catch (MissingResourceException e) {
      return code;
    }
  }
}
