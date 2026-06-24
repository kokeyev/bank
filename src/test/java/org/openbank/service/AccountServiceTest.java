package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.status.AccountStatus;
import org.openbank.service.impl.AccountServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  private static final Long ACCOUNT_ID = 1L;
  private static final Long PENDING_ACCOUNT_ID = 11L;
  private static final Long USER_ID = 7L;
  private static final Long OTHER_USER_ID = 99L;
  private static final Long PENDING_USER_ID = 3L;
  private static final Long CURRENCY_ID = 1L;
  private static final String CURRENCY_NAME = "KZT";
  private static final String ACCOUNT_NAME = "Main";
  private static final String FIRST_CARD_NUMBER = "4000000000000002";
  private static final String SECOND_CARD_NUMBER = "4000000000000010";
  private static final String CVV = "123";
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2030, 1, 1);
  private static final BigDecimal TRANSACTION_LIMIT = new BigDecimal("100000");
  private static final BigDecimal BALANCE_50 = new BigDecimal("50");
  private static final BigDecimal BALANCE_100 = new BigDecimal("100");
  private static final BigDecimal WITHDRAW_AMOUNT_60 = new BigDecimal("60");
  private static final long NO_ACTIVE_ACCOUNTS = 0L;
  private static final long MAX_ACTIVE_ACCOUNTS = 10L;

  @Mock
  private AccountDao accountDao;

  @Mock
  private CurrencyDao currencyDao;

  @Mock
  private BankCardGenerator bankCardGenerator;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private AccountServiceImpl service;

  @Test
  void createNewAccountGeneratesUniqueCardAndCreatesPendingAccount() {
    Currency kzt = new Currency(CURRENCY_ID, CURRENCY_NAME, BigDecimal.ONE);
    when(currencyDao.getCurrencyByName(CURRENCY_NAME)).thenReturn(Optional.of(kzt));
    when(bankCardGenerator.generateCardNumber()).thenReturn(FIRST_CARD_NUMBER);
    when(bankCardGenerator.generateCvv()).thenReturn(CVV);
    when(bankCardGenerator.generateExpiryDate()).thenReturn(EXPIRY_DATE);
    when(accountDao.getAccountByCardNumber(FIRST_CARD_NUMBER)).thenReturn(Optional.empty());

    service.createNewAccount(USER_ID, CURRENCY_NAME, TRANSACTION_LIMIT, ACCOUNT_NAME);

    verify(accountDao).createNewAccount(
        eq(USER_ID),
        eq(FIRST_CARD_NUMBER),
        eq(CVV),
        eq(EXPIRY_DATE),
        eq(BigDecimal.ZERO),
        eq(CURRENCY_ID),
        eq(AccountStatus.PENDING),
        eq(TRANSACTION_LIMIT),
        eq(ACCOUNT_NAME),
        eq(false)
    );
  }

  @Test
  void createNewAccountRetriesWhenGeneratedCardAlreadyExists() {
    Currency kzt = new Currency(CURRENCY_ID, CURRENCY_NAME, BigDecimal.ONE);
    when(currencyDao.getCurrencyByName(CURRENCY_NAME)).thenReturn(Optional.of(kzt));
    when(bankCardGenerator.generateCardNumber()).thenReturn(FIRST_CARD_NUMBER, SECOND_CARD_NUMBER);
    when(bankCardGenerator.generateCvv()).thenReturn(CVV);
    when(bankCardGenerator.generateExpiryDate()).thenReturn(EXPIRY_DATE);
    when(accountDao.getAccountByCardNumber(FIRST_CARD_NUMBER)).thenReturn(Optional.of(activeAccount(BigDecimal.TEN, BigDecimal.TEN)));
    when(accountDao.getAccountByCardNumber(SECOND_CARD_NUMBER)).thenReturn(Optional.empty());

    service.createNewAccount(USER_ID, CURRENCY_NAME, BigDecimal.TEN, ACCOUNT_NAME);

    verify(accountDao).createNewAccount(
        eq(USER_ID),
        eq(SECOND_CARD_NUMBER),
        eq(CVV),
        eq(EXPIRY_DATE),
        eq(BigDecimal.ZERO),
        eq(CURRENCY_ID),
        eq(AccountStatus.PENDING),
        eq(BigDecimal.TEN),
        eq(ACCOUNT_NAME),
        eq(false)
    );
  }

  @Test
  void withdrawRejectsInsufficientBalance() {
    Account account = activeAccount(BALANCE_50, BALANCE_100);
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Executable executable = () -> service.withdraw(ACCOUNT_ID, WITHDRAW_AMOUNT_60);
    assertThrows(IllegalArgumentException.class, executable);
    verify(accountDao, never()).withdraw(any(), any());
  }

  @Test
  void approveFirstAccountMakesItMain() {
    Account account = pendingAccount(PENDING_ACCOUNT_ID, PENDING_USER_ID);
    when(accountDao.getAccountById(PENDING_ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.countAccountsByUserIdAndStatus(PENDING_USER_ID, AccountStatus.ACTIVE)).thenReturn(NO_ACTIVE_ACCOUNTS);
    when(accountDao.setStatusToAccount(PENDING_ACCOUNT_ID, AccountStatus.PENDING, AccountStatus.ACTIVE)).thenReturn(true);

    service.approveAccount(PENDING_ACCOUNT_ID);

    verify(accountDao).clearMainAccount(PENDING_USER_ID);
    verify(accountDao).setMainAccount(PENDING_ACCOUNT_ID);
  }

  @Test
  void approveAccountRejectsWhenClientAlreadyHasMaximumActiveAccounts() {
    Account account = pendingAccount(PENDING_ACCOUNT_ID, PENDING_USER_ID);
    when(accountDao.getAccountById(PENDING_ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.countAccountsByUserIdAndStatus(PENDING_USER_ID, AccountStatus.ACTIVE)).thenReturn(MAX_ACTIVE_ACCOUNTS);

    Executable executable = () -> service.approveAccount(PENDING_ACCOUNT_ID);
    assertThrows(IllegalArgumentException.class, executable);

    verify(accountDao).setStatusToAccount(PENDING_ACCOUNT_ID, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  @Test
  void updateTransactionLimitRejectsAccountOwnedByAnotherUser() {
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(activeAccount(BigDecimal.TEN, BigDecimal.TEN)));

    Executable executable = () -> service.updateTransactionLimit(OTHER_USER_ID, ACCOUNT_ID, BigDecimal.ONE);
    assertThrows(IllegalArgumentException.class, executable);

    verify(accountDao, never()).updateTransactionLimit(any(), any());
  }

  @Test
  void deactivateMainAccountClearsMainAccount() {
    Account account = activeAccount(BigDecimal.TEN, BigDecimal.TEN);
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.setStatusToAccount(ACCOUNT_ID, AccountStatus.DEACTIVATED)).thenReturn(true);

    service.deactivateAccount(USER_ID, ACCOUNT_ID);

    verify(accountDao).clearMainAccount(USER_ID);
  }

  @Test
  void makeMainAccountClearsOldMainAndSetsNewMain() {
    Account account = activeAccount(BigDecimal.TEN, BigDecimal.TEN);
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.setMainAccount(ACCOUNT_ID)).thenReturn(true);

    service.makeMainAccount(USER_ID, ACCOUNT_ID);

    verify(accountDao).clearMainAccount(USER_ID);
    verify(accountDao).setMainAccount(ACCOUNT_ID);
  }

  @Test
  void topUpRejectsAmountAboveTransactionLimit() {
    Account account = activeAccount(BigDecimal.TEN, BigDecimal.ONE);
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account));

    Executable executable = () -> service.topUp(ACCOUNT_ID, BigDecimal.TEN);
    assertThrows(IllegalArgumentException.class, executable);

    verify(accountDao, never()).topUp(any(), any());
  }

  private Account activeAccount(BigDecimal balance, BigDecimal limit) {
    return new Account(ACCOUNT_ID, USER_ID, FIRST_CARD_NUMBER, CVV, LocalDate.now().plusYears(1), balance, CURRENCY_ID, AccountStatus.ACTIVE.name(), limit, ACCOUNT_NAME, true);
  }

  private Account pendingAccount(Long accountId, Long userId) {
    return new Account(accountId, userId, FIRST_CARD_NUMBER, CVV, LocalDate.now().plusYears(1), BigDecimal.ZERO, CURRENCY_ID, AccountStatus.PENDING.name(), BigDecimal.TEN, ACCOUNT_NAME, false);
  }
}
