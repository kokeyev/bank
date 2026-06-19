package org.openbank.service;

import org.springframework.context.i18n.LocaleContextHolder;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Static message lookup used by lower-level services that are not passed {@link MessageService}.
 *
 * <p>When a key is missing the code itself is returned, which keeps validation failures readable
 * during development instead of hiding the original key.</p>
 */
public final class Messages {

  private Messages() {
  }

  /**
   * Resolves a localized message from the active request locale.
   *
   * @param code key from the message bundle
   * @param args optional values for {@link MessageFormat}
   * @return localized message, or the key itself when the bundle/key is missing
   */
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
