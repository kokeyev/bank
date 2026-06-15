package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.account.AccountDao;
import org.openbank.dao.currency.CurrencyDao;
import org.openbank.dao.loan.LoanDao;
import org.openbank.dao.transaction.TransactionDao;
import org.openbank.dao.user.UserDao;
import org.openbank.model.Account;
import org.openbank.model.Currency;
import org.openbank.model.Loan;
import org.openbank.model.User;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.LoanStatus;

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
  private TransactionService service;

  @BeforeEach
  void runCallbacks() {
    lenient().when(transactionRunner.run(anyString(), any())).thenAnswer(invocation -> {
      DatabaseTransactionRunner.TransactionCallback<?> callback = invocation.getArgument(1);
      return callback.execute(connection);
    });
  }

  @Test
  void transferBetweenOwnAccountsWithdrawsFeeAndConvertsAmount() {
    Account sender = account(1L, 7L, 1L, new BigDecimal("1000"), new BigDecimal("1000"));
    Account receiver = account(2L, 7L, 2L, BigDecimal.ZERO, new BigDecimal("1000"));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, 2L)).thenReturn(Optional.of(receiver));
    when(bankSettingsService.calculateTransferFee(new BigDecimal("100"))).thenReturn(new BigDecimal("2"));
    when(currencyDao.getCurrencyRateToKztById(1L)).thenReturn(new BigDecimal("500"));
    when(currencyDao.getCurrencyRateToKztById(2L)).thenReturn(new BigDecimal("250"));
    when(accountDao.withdraw(connection, 1L, new BigDecimal("102"))).thenReturn(true);
    when(accountDao.topUp(connection, 2L, new BigDecimal("200.00"))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(2L), any(), eq(new BigDecimal("100")), eq(1L), eq(new BigDecimal("2")), eq("hello"), eq("BETWEEN_OWN_ACCOUNTS"))).thenReturn(true);

    service.makeTransactionBetweenAccountsOfOneClient(7L, 1L, 2L, new BigDecimal("100"), "hello");

    verify(accountDao).withdraw(connection, 1L, new BigDecimal("102"));
    verify(accountDao).topUp(connection, 2L, new BigDecimal("200.00"));
  }

  @Test
  void phoneTransferUsesReceiversMainAccount() {
    User receiver = user(9L);
    Account sender = account(1L, 7L, 1L, new BigDecimal("1000"), new BigDecimal("1000"));
    Account receiverAccount = account(3L, 9L, 1L, BigDecimal.ZERO, new BigDecimal("1000"));
    when(userDao.getUserByPhoneNumber("+77001112233")).thenReturn(Optional.of(receiver));
    when(accountDao.getMainActiveAccountByUserId(9L)).thenReturn(Optional.of(receiverAccount));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(sender));
    when(accountDao.getAccountByIdForUpdate(connection, 3L)).thenReturn(Optional.of(receiverAccount));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, 1L, BigDecimal.TEN)).thenReturn(true);
    when(accountDao.topUp(connection, 3L, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(3L), any(), eq(BigDecimal.TEN), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("PHONE_TRANSFER"))).thenReturn(true);

    service.makeTransactionByPhoneNumber(1L, "8 700 111 22 33", BigDecimal.TEN);

    verify(userDao).getUserByPhoneNumber("+77001112233");
  }

  @Test
  void cardTransferToUnknownCardCreatesExternalTransfer() {
    Account sender = account(1L, 7L, 1L, new BigDecimal("1000"), new BigDecimal("1000"));
    when(accountDao.getAccountByCardNumber("5555444433332222")).thenReturn(Optional.empty());
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(sender));
    when(bankSettingsService.calculateTransferFee(BigDecimal.TEN)).thenReturn(BigDecimal.ZERO);
    when(accountDao.withdraw(connection, 1L, BigDecimal.TEN)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(null), any(), eq(BigDecimal.TEN), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("EXTERNAL_CARD_TRANSFER"))).thenReturn(true);

    service.makeTransactionByCardNumber(1L, "5555 4444 3333 2222", BigDecimal.TEN);

    verify(accountDao).withdraw(connection, 1L, BigDecimal.TEN);
  }

  @Test
  void loanPaymentConvertsToKztAndPaysLoan() {
    Account sender = account(1L, 7L, 2L, new BigDecimal("1000"), new BigDecimal("1000"));
    Loan loan = new Loan(4L, 7L, 1L, null, new BigDecimal("500"), BigDecimal.TEN, 12, LoanStatus.ACTIVE.name(), LocalDate.now(), BigDecimal.TEN);
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(sender));
    when(loanDao.getLoanById(4L)).thenReturn(Optional.of(loan));
    when(currencyDao.getCurrencyByName("KZT")).thenReturn(Optional.of(new Currency(1L, "KZT", BigDecimal.ONE)));
    when(currencyDao.getCurrencyRateToKztById(2L)).thenReturn(new BigDecimal("500"));
    when(currencyDao.getCurrencyRateToKztById(1L)).thenReturn(BigDecimal.ONE);
    when(accountDao.withdraw(connection, 1L, BigDecimal.TEN)).thenReturn(true);
    when(loanDao.payLoan(connection, 4L, new BigDecimal("5000.00"))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(null), any(), eq(BigDecimal.TEN), eq(2L), eq(BigDecimal.ZERO), anyString(), eq("LOAN_PAYMENT"))).thenReturn(true);

    service.makeTransactionTopUpLoan(1L, 4L, BigDecimal.TEN);

    verify(loanDao).payLoan(connection, 4L, new BigDecimal("5000.00"));
  }

  @Test
  void transferRejectsSameAccount() {
    assertThrows(IllegalArgumentException.class, () -> service.makeTransactionBetweenAccountsOfOneClient(7L, 1L, 1L, BigDecimal.TEN, ""));
  }

  private Account account(Long accountId, Long userId, Long currencyId, BigDecimal balance, BigDecimal limit) {
    return new Account(accountId, userId, "4000000000000002", "123", LocalDate.now().plusYears(1), balance, currencyId, AccountStatus.ACTIVE.name(), limit, "Main", true);
  }

  private User user(Long userId) {
    return new User(userId, "A", "B", "+77001112233", "a@b.kz", "CLIENT", "ACTIVE", LocalDate.now(), null, "hash");
  }
}
