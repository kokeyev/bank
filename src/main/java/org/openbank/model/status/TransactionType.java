package org.openbank.model.status;

import org.openbank.service.MessageService;

public enum TransactionType {
  BETWEEN_OWN_ACCOUNTS("transaction.type.betweenOwnAccounts"),
  PHONE_TRANSFER("transaction.type.phoneTransfer"),
  CARD_TRANSFER("transaction.type.cardTransfer"),
  EXTERNAL_CARD_TRANSFER("transaction.type.externalCardTransfer"),
  CURRENCY_EXCHANGE("transaction.type.currencyExchange"),
  LOAN_PAYMENT("transaction.type.loanPayment"),
  ACCOUNT_TOP_UP("transaction.type.accountTopUp"),
  LOAN_DISBURSEMENT("transaction.type.loanDisbursement"),
  DEPOSIT_OPEN("transaction.type.depositOpen"),
  DEPOSIT_TOP_UP("transaction.type.depositTopUp"),
  DEPOSIT_WITHDRAWAL("transaction.type.depositWithdrawal"),
  DEPOSIT_INTEREST("transaction.type.depositInterest"),
  DEPOSIT_REJECTION_REFUND("transaction.type.depositRejectionRefund");

  private final String messageKey;

  TransactionType(String messageKey) {
    this.messageKey = messageKey;
  }

  public String displayName(MessageService messageService) {
    return messageService.get(messageKey);
  }

  public static String displayName(String type, MessageService messageService) {
    if (type == null || type.isBlank()) {
      return "";
    }

    try {
      return TransactionType.valueOf(type).displayName(messageService);
    } catch (IllegalArgumentException e) {
      return type;
    }
  }
}
