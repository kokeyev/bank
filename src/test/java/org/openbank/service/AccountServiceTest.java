package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.account.AccountDao;
import org.openbank.dao.currency.CurrencyDao;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.status.AccountStatus;

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

  @Mock
  private AccountDao accountDao;

  @Mock
  private CurrencyDao currencyDao;

  @Mock
  private BankCardGenerator bankCardGenerator;

  @InjectMocks
  private AccountService service;

  @Test
  void createNewAccountGeneratesUniqueCardAndCreatesPendingAccount() {
    Currency kzt = new Currency(1L, "KZT", BigDecimal.ONE);
    when(currencyDao.getCurrencyByName("KZT")).thenReturn(Optional.of(kzt));
    when(bankCardGenerator.generateCardNumber()).thenReturn("4000000000000002");
    when(bankCardGenerator.generateCvv()).thenReturn("123");
    when(bankCardGenerator.generateExpiryDate()).thenReturn(LocalDate.of(2030, 1, 1));
    when(accountDao.getAccountByCardNumber("4000000000000002")).thenReturn(Optional.empty());

    service.createNewAccount(7L, "KZT", new BigDecimal("100000"), "Main");

    verify(accountDao).createNewAccount(
        eq(7L),
        eq("4000000000000002"),
        eq("123"),
        eq(LocalDate.of(2030, 1, 1)),
        eq(BigDecimal.ZERO),
        eq(1L),
        eq(AccountStatus.PENDING),
        eq(new BigDecimal("100000")),
        eq("Main"),
        eq(false)
    );
  }

  @Test
  void withdrawRejectsInsufficientBalance() {
    Account account = activeAccount(new BigDecimal("50"), new BigDecimal("100"));
    when(accountDao.getAccountById(1L)).thenReturn(Optional.of(account));

    assertThrows(IllegalArgumentException.class, () -> service.withdraw(1L, new BigDecimal("60")));
    verify(accountDao, never()).withdraw(any(), any());
  }

  @Test
  void approveFirstAccountMakesItMain() {
    Account account = pendingAccount(11L, 3L);
    when(accountDao.getAccountById(11L)).thenReturn(Optional.of(account));
    when(accountDao.countAccountsByUserIdAndStatus(3L, AccountStatus.ACTIVE)).thenReturn(0L);
    when(accountDao.setStatusToAccount(11L, AccountStatus.PENDING, AccountStatus.ACTIVE)).thenReturn(true);

    service.approveAccount(11L);

    verify(accountDao).clearMainAccount(3L);
    verify(accountDao).setMainAccount(11L);
  }

  @Test
  void approveAccountRejectsWhenClientAlreadyHasMaximumActiveAccounts() {
    Account account = pendingAccount(11L, 3L);
    when(accountDao.getAccountById(11L)).thenReturn(Optional.of(account));
    when(accountDao.countAccountsByUserIdAndStatus(3L, AccountStatus.ACTIVE)).thenReturn(10L);

    assertThrows(IllegalArgumentException.class, () -> service.approveAccount(11L));

    verify(accountDao).setStatusToAccount(11L, AccountStatus.PENDING, AccountStatus.REJECTED);
  }

  private Account activeAccount(BigDecimal balance, BigDecimal limit) {
    return new Account(1L, 7L, "4000000000000002", "123", LocalDate.now().plusYears(1), balance, 1L, AccountStatus.ACTIVE.name(), limit, "Main", true);
  }

  private Account pendingAccount(Long accountId, Long userId) {
    return new Account(accountId, userId, "4000000000000002", "123", LocalDate.now().plusYears(1), BigDecimal.ZERO, 1L, AccountStatus.PENDING.name(), BigDecimal.TEN, "Main", false);
  }
}
