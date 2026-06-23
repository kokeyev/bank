package org.openbank.view;

import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

@Component
public class TransactionTypeFormatter {

  private final MessageService messageService;

  public TransactionTypeFormatter(MessageService messageService) {
    this.messageService = messageService;
  }

  public String displayName(String type) {
    if ("BETWEEN_OWN_ACCOUNTS".equals(type)) {
      return messageService.get("transaction.type.betweenOwnAccounts");
    }
    if ("PHONE_TRANSFER".equals(type)) {
      return messageService.get("transaction.type.phoneTransfer");
    }
    if ("CARD_TRANSFER".equals(type)) {
      return messageService.get("transaction.type.cardTransfer");
    }
    if ("EXTERNAL_CARD_TRANSFER".equals(type)) {
      return messageService.get("transaction.type.externalCardTransfer");
    }
    if ("CURRENCY_EXCHANGE".equals(type)) {
      return messageService.get("transaction.type.currencyExchange");
    }
    if ("LOAN_PAYMENT".equals(type)) {
      return messageService.get("transaction.type.loanPayment");
    }
    if ("ACCOUNT_TOP_UP".equals(type)) {
      return messageService.get("transaction.type.accountTopUp");
    }
    if ("LOAN_DISBURSEMENT".equals(type)) {
      return messageService.get("transaction.type.loanDisbursement");
    }
    if ("DEPOSIT_OPEN".equals(type)) {
      return messageService.get("transaction.type.depositOpen");
    }
    if ("DEPOSIT_TOP_UP".equals(type)) {
      return messageService.get("transaction.type.depositTopUp");
    }
    if ("DEPOSIT_WITHDRAWAL".equals(type)) {
      return messageService.get("transaction.type.depositWithdrawal");
    }
    if ("DEPOSIT_INTEREST".equals(type)) {
      return messageService.get("transaction.type.depositInterest");
    }
    return type == null ? "" : type;
  }
}
