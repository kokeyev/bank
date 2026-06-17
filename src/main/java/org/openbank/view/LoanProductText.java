package org.openbank.view;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LoanProductText {

  private static final String AUTO = "Автокредит";
  private static final String MORTGAGE = "Ипотека";

  private final MessageSource messageSource;

  public LoanProductText(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String slug(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "auto";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "mortgage";
    }
    return "purpose";
  }

  public String name(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".name");
  }

  public String tag(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".tag");
  }

  public String description(String loanTypeName) {
    return message(keyPrefix(loanTypeName) + ".description");
  }

  public String amountRange(String minimumAmount, String maximumAmount) {
    return message("loans.amount.range", minimumAmount, maximumAmount);
  }

  public String durationUpTo(Integer durationMonths) {
    return message("loans.duration.upTo", durationMonths);
  }

  public String rateFrom(String rate) {
    return message("loans.rate.from", rate);
  }

  public String remainingAmount(String loanTypeName, String remainingAmount) {
    return message("loans.remaining", name(loanTypeName), remainingAmount);
  }

  private String keyPrefix(String loanTypeName) {
    return "loans." + slug(loanTypeName);
  }

  private String message(String code, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(code, args, locale);
  }
}
