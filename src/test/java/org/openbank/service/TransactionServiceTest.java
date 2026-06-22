package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.dao.LoanDao;
import org.openbank.dao.TransactionDao;
import org.openbank.dao.UserDao;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.Loan;
import org.openbank.model.User;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.impl.TransactionServiceImpl;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  private static final Long SENDER_ACCOUNT_ID = 1L;
  private static final Long RECEIVER_ACCOUNT_ID = 2L;
  private static final Long PHONE_RECEIVER_ACCOUNT_ID = 3L;
  private static final Long LOAN_ID = 4L;
  private static final Long USER_ID = 7L;
  private static final Long RECEIVER_USER_ID = 9L;
  private static final Long KZT_CURRENCY_ID = 1L;
  private static final Long USD_CURRENCY_ID = 2L;
  private static final String KZT_CURRENCY_NAME = "KZT";
  private static final String CARD_NUMBER = "4000000000000002";
  private static final String FORMATTED_CARD_NUMBER = "4000 0000 0000 0002";
  private static final String EXTERNAL_CARD_NUMBER = "5555444433332222";
  private static final String FORMATTED_EXTERNAL_CARD_NUMBER = "5555 4444 3333 2222";
  private static final String PHONE_NUMBER = "+77001112233";
  private static final String PHONE_NUMBER_FROM_EIGHT = "8 700 111 22 33";
  private static final String PHONE_NUMBER_WITHOUT_PLUS = "77001112233";
  private static final String MISSING_PHONE_NUMBER = "+77001112234";
  private static final String TRANSFER_MESSAGE = "hello";
  private static final String EMPTY_MESSAGE = "";
  private static final String BETWEEN_OWN_ACCOUNTS_TYPE = "BETWEEN_OWN_ACCOUNTS";
  private static final String PHONE_TRANSFER_TYPE = "PHONE_TRANSFER";
  private static final String CARD_TRANSFER_TYPE = "CARD_TRANSFER";
  private static final String EXTERNAL_CARD_TRANSFER_TYPE = "EXTERNAL_CARD_TRANSFER";
  private static final String LOAN_PAYMENT_TYPE = "LOAN_PAYMENT";
  private static final String CVV = "123";
  private static final String ACCOUNT_NAME = "Main";
  private static final String USER_NAME = "A";
  private static final String USER_SURNAME = "B";
  private static final String USER_EMAIL = "a@b.kz";
  private static final String CLIENT_ROLE = "CLIENT";
  private static final String ACTIVE_STATUS = "ACTIVE";
  private static final String PASSWORD_HASH = "hash";
  private static final BigDecimal ACCOUNT_BALANCE = new BigDecimal("1000");
  private static final BigDecimal ACCOUNT_LIMIT = new BigDecimal("1000");
  private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("100");
  private static final BigDecimal TRANSFER_FEE = new BigDecimal("2");
  private static final BigDecimal DEBIT_AMOUNT_WITH_FEE = new BigDecimal("102");
  private static final BigDecimal FROM_RATE_TO_KZT = new BigDecimal("500");
  private static final BigDecimal TO_RATE_TO_KZT = new BigDecimal("250");
  private static final BigDecimal CONVERTED_TRANSFER_AMOUNT = new BigDecimal("200.00");
  private static final BigDecimal LOAN_REMAINING_AMOUNT = new BigDecimal("500");
  private static final BigDecimal EXPECTED_LOAN_PAYMENT = new BigDecimal("5000.00");
  private static final int LOAN_DURATION = 12;

  @Mock
  private AccountDao accountDao;

  @Mock
  private TransactionDao transactionDao;

  @Mock
  private CurrencyDao currencyDao;

  @Mock
  private UserDao userDao;

  @Mock
  private LoanDao loanDao;

  @Mock
  private BankSettingsService bankSettingsService;

  @Mock
  private DatabaseTransactionRunner transactionRunner;

  @Mock
  private Connection connection;

  @InjectMocks
  private TransactionServiceImpl service;

  @BeforeEach
  void runCallbacks() {
    lenient().when(transactionRunner.run(anyString(), any())).thenAnswer(invocation -> {
      DatabaseTransactionRunner.TransactionCallback<?> callback = invocation.getArgument(1);
      return callback.execute(connection);
    });
  }

  @Test
  void transferBetweenOwnAccountsWithdrawsFeeAndConvertsAmount() {
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, KZT_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    Account receiver = account(RECEIVER_ACCOUNT_ID, USER_ID, USD_CURRENCY_ID, BigDecimal.ZERO, ACCOUNT_LIMIT);
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, RECEIVER_ACCOUNT_ID)).thenReturn(Optional.of(receiver));
    when(bankSettingsService.calculateTransferFee(TRANSFER_AMOUNT)).thenReturn(TRANSFER_FEE);
    when(currencyDao.getCurrencyRateToKztById(KZT_CURRENCY_ID)).thenReturn(FROM_RATE_TO_KZT);
    when(currencyDao.getCurrencyRateToKztById(USD_CURRENCY_ID)).thenReturn(TO_RATE_TO_KZT);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, DEBIT_AMOUNT_WITH_FEE)).thenReturn(true);
    when(accountDao.topUp(connection, RECEIVER_ACCOUNT_ID, CONVERTED_TRANSFER_AMOUNT)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(RECEIVER_ACCOUNT_ID), any(), eq(TRANSFER_AMOUNT), eq(KZT_CURRENCY_ID), eq(TRANSFER_FEE), eq(TRANSFER_MESSAGE), eq(BETWEEN_OWN_ACCOUNTS_TYPE))).thenReturn(true);

    service.makeTransactionBetweenAccountsOfOneClient(USER_ID, SENDER_ACCOUNT_ID, RECEIVER_ACCOUNT_ID, TRANSFER_AMOUNT, TRANSFER_MESSAGE);

    verify(accountDao).withdraw(connection, SENDER_ACCOUNT_ID, DEBIT_AMOUNT_WITH_FEE);
    verify(accountDao).topUp(connection, RECEIVER_ACCOUNT_ID, CONVERTED_TRANSFER_AMOUNT);
  }

  @Test
  void phoneTransferUsesReceiversMainAccount() {
    User receiver = user(RECEIVER_USER_ID);
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, KZT_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    Account receiverAccount = account(PHONE_RECEIVER_ACCOUNT_ID, RECEIVER_USER_ID, KZT_CURRENCY_ID, BigDecimal.ZERO, ACCOUNT_LIMIT);
    when(userDao.getUserByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(receiver));
    when(accountDao.getMainActiveAccountByUserId(RECEIVER_USER_ID)).thenReturn(Optional.of(receiverAccount));
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, PHONE_RECEIVER_ACCOUNT_ID)).thenReturn(Optional.of(receiverAccount));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(accountDao.topUp(connection, PHONE_RECEIVER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(PHONE_RECEIVER_ACCOUNT_ID), any(), eq(BigDecimal.TEN), eq(KZT_CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(PHONE_TRANSFER_TYPE))).thenReturn(true);

    service.makeTransactionByPhoneNumber(SENDER_ACCOUNT_ID, PHONE_NUMBER_FROM_EIGHT, BigDecimal.TEN);

    verify(userDao).getUserByPhoneNumber(PHONE_NUMBER);
  }

  @Test
  void cardTransferToUnknownCardCreatesExternalTransfer() {
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, KZT_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    when(accountDao.getAccountByCardNumber(EXTERNAL_CARD_NUMBER)).thenReturn(Optional.empty());
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(null), any(), eq(BigDecimal.TEN), eq(KZT_CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(EXTERNAL_CARD_TRANSFER_TYPE))).thenReturn(true);

    service.makeTransactionByCardNumber(SENDER_ACCOUNT_ID, FORMATTED_EXTERNAL_CARD_NUMBER, BigDecimal.TEN);

    verify(accountDao).withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN);
  }

  @Test
  void cardTransferToKnownCardUsesInternalTransfer() {
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, KZT_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    Account receiver = account(RECEIVER_ACCOUNT_ID, RECEIVER_USER_ID, KZT_CURRENCY_ID, BigDecimal.ZERO, ACCOUNT_LIMIT);
    when(accountDao.getAccountByCardNumber(CARD_NUMBER)).thenReturn(Optional.of(receiver));
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, RECEIVER_ACCOUNT_ID)).thenReturn(Optional.of(receiver));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(accountDao.topUp(connection, RECEIVER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(RECEIVER_ACCOUNT_ID), any(), eq(BigDecimal.TEN), eq(KZT_CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(CARD_TRANSFER_TYPE))).thenReturn(true);

    service.makeTransactionByCardNumber(SENDER_ACCOUNT_ID, FORMATTED_CARD_NUMBER, BigDecimal.TEN);

    verify(accountDao).topUp(connection, RECEIVER_ACCOUNT_ID, BigDecimal.TEN);
  }

  @Test
  void phoneTransferFallsBackToFirstActiveAccount() {
    User receiver = user(RECEIVER_USER_ID);
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, KZT_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    Account receiverAccount = account(PHONE_RECEIVER_ACCOUNT_ID, RECEIVER_USER_ID, KZT_CURRENCY_ID, BigDecimal.ZERO, ACCOUNT_LIMIT);
    when(userDao.getUserByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(receiver));
    when(accountDao.getMainActiveAccountByUserId(RECEIVER_USER_ID)).thenReturn(Optional.empty());
    when(accountDao.getFirstActiveAccountByUserId(RECEIVER_USER_ID)).thenReturn(Optional.of(receiverAccount));
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, PHONE_RECEIVER_ACCOUNT_ID)).thenReturn(Optional.of(receiverAccount));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(accountDao.topUp(connection, PHONE_RECEIVER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(PHONE_RECEIVER_ACCOUNT_ID), any(), eq(BigDecimal.TEN), eq(KZT_CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(PHONE_TRANSFER_TYPE))).thenReturn(true);

    service.makeTransactionByPhoneNumber(SENDER_ACCOUNT_ID, PHONE_NUMBER_WITHOUT_PLUS, BigDecimal.TEN);

    verify(accountDao).getFirstActiveAccountByUserId(RECEIVER_USER_ID);
  }

  @Test
  void phoneTransferRejectsMissingReceiverOrReceiverWithoutAccount() {
    when(userDao.getUserByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.makeTransactionByPhoneNumber(SENDER_ACCOUNT_ID, PHONE_NUMBER, BigDecimal.TEN));

    User receiver = user(RECEIVER_USER_ID);
    when(userDao.getUserByPhoneNumber(MISSING_PHONE_NUMBER)).thenReturn(Optional.of(receiver));
    when(accountDao.getMainActiveAccountByUserId(RECEIVER_USER_ID)).thenReturn(Optional.empty());
    when(accountDao.getFirstActiveAccountByUserId(RECEIVER_USER_ID)).thenReturn(Optional.empty());

    assertThrows(IllegalArgumentException.class, () -> service.makeTransactionByPhoneNumber(SENDER_ACCOUNT_ID, MISSING_PHONE_NUMBER, BigDecimal.TEN));
  }

  @Test
  void loanPaymentConvertsToKztAndPaysLoan() {
    Account sender = account(SENDER_ACCOUNT_ID, USER_ID, USD_CURRENCY_ID, ACCOUNT_BALANCE, ACCOUNT_LIMIT);
    Loan loan = new Loan(LOAN_ID, USER_ID, KZT_CURRENCY_ID, null, LOAN_REMAINING_AMOUNT, BigDecimal.TEN, LOAN_DURATION, LoanStatus.ACTIVE.name(), LocalDate.now(), BigDecimal.TEN);
    when(accountDao.getAccountByIdForUpdate(connection, SENDER_ACCOUNT_ID)).thenReturn(Optional.of(sender));
    when(loanDao.getLoanById(LOAN_ID)).thenReturn(Optional.of(loan));
    when(currencyDao.getCurrencyByName(KZT_CURRENCY_NAME)).thenReturn(Optional.of(new Currency(KZT_CURRENCY_ID, KZT_CURRENCY_NAME, BigDecimal.ONE)));
    when(currencyDao.getCurrencyRateToKztById(USD_CURRENCY_ID)).thenReturn(FROM_RATE_TO_KZT);
    when(currencyDao.getCurrencyRateToKztById(KZT_CURRENCY_ID)).thenReturn(BigDecimal.ONE);
    when(accountDao.withdraw(connection, SENDER_ACCOUNT_ID, BigDecimal.TEN)).thenReturn(true);
    when(loanDao.payLoan(connection, LOAN_ID, EXPECTED_LOAN_PAYMENT)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(SENDER_ACCOUNT_ID), eq(null), any(), eq(BigDecimal.TEN), eq(USD_CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(LOAN_PAYMENT_TYPE))).thenReturn(true);

    service.makeTransactionTopUpLoan(SENDER_ACCOUNT_ID, LOAN_ID, BigDecimal.TEN);

    verify(loanDao).payLoan(connection, LOAN_ID, EXPECTED_LOAN_PAYMENT);
  }

  @Test
  void transferRejectsSameAccount() {
    assertThrows(IllegalArgumentException.class, () -> service.makeTransactionBetweenAccountsOfOneClient(USER_ID, SENDER_ACCOUNT_ID, SENDER_ACCOUNT_ID, BigDecimal.TEN, EMPTY_MESSAGE));
  }

  private Account account(Long accountId, Long userId, Long currencyId, BigDecimal balance, BigDecimal limit) {
    return new Account(accountId, userId, CARD_NUMBER, CVV, LocalDate.now().plusYears(1), balance, currencyId, AccountStatus.ACTIVE.name(), limit, ACCOUNT_NAME, true);
  }

  private User user(Long userId) {
    return new User(userId, USER_NAME, USER_SURNAME, PHONE_NUMBER, USER_EMAIL, CLIENT_ROLE, ACTIVE_STATUS, LocalDate.now(), null, PASSWORD_HASH);
  }
}
