package org.openbank.view;

import org.springframework.stereotype.Component;

@Component
public class TransactionTypeFormatter {

  public String displayName(String type) {
    if ("BETWEEN_OWN_ACCOUNTS".equals(type)) {
      return "Между своими счетами";
    }
    if ("PHONE_TRANSFER".equals(type)) {
      return "По телефону";
    }
    if ("CARD_TRANSFER".equals(type)) {
      return "На карту";
    }
    if ("EXTERNAL_CARD_TRANSFER".equals(type)) {
      return "В другой банк";
    }
    if ("CURRENCY_EXCHANGE".equals(type)) {
      return "Обмен валют";
    }
    if ("LOAN_PAYMENT".equals(type)) {
      return "Платеж по кредиту";
    }
    if ("DEPOSIT_OPEN".equals(type)) {
      return "Открытие депозита";
    }
    if ("DEPOSIT_TOP_UP".equals(type)) {
      return "Пополнение депозита";
    }
    if ("DEPOSIT_WITHDRAWAL".equals(type)) {
      return "Снятие с депозита";
    }
    if ("DEPOSIT_INTEREST".equals(type)) {
      return "Вознаграждение";
    }
    return type == null ? "" : type;
  }
}
