package org.openbank.service;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resolves localized messages for controllers and validation flows.
 *
 * <p>The locale is read from Spring's {@link LocaleContextHolder}, which is populated by the
 * locale interceptor configured for the web layer.</p>
 */
@Service
public class MessageService {

  private final MessageSource messageSource;

  public MessageService(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Resolves a message code with optional formatting arguments.
   *
   * @param code key from the active message bundle
   * @param args optional values used by the message pattern
   * @return localized message for the current request locale
   */
  public String get(String code, Object... args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }
}
