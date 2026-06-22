package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.dao.DepositDao;
import org.openbank.dao.DepositTypeDao;
import org.openbank.dao.TransactionDao;
import org.openbank.dto.OpenDepositRequest;
import org.openbank.model.Account;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.DepositStatus;
import org.openbank.service.impl.DepositServiceImpl;
import org.openbank.service.strategy.deposit.CapitalDepositStrategy;
import org.openbank.service.strategy.deposit.DepositProductStrategyResolver;
import org.openbank.service.strategy.deposit.KopilkaDepositStrategy;
import org.openbank.service.strategy.deposit.StrategyDepositStrategy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

  @Mock
  private DepositDao depositDao;

  @Mock
  private DepositTypeDao depositTypeDao;

  @Mock
  private AccountDao accountDao;

  @Mock
  private CurrencyDao currencyDao;

  @Mock
  private TransactionDao transactionDao;

  @Mock
  private DatabaseTransactionRunner transactionRunner;

  @Mock
  private Connection connection;

  private DepositService service;

  @BeforeEach
  void runCallbacks() {
    DepositProductStrategyResolver strategyResolver = new DepositProductStrategyResolver(List.of(
        new KopilkaDepositStrategy(),
        new StrategyDepositStrategy(),
        new CapitalDepositStrategy()
    ));
    service = new DepositServiceImpl(depositDao, depositTypeDao, accountDao, currencyDao, transactionDao, transactionRunner, strategyResolver);

    when(transactionRunner.run(anyString(), any())).thenAnswer(invocation -> {
      DatabaseTransactionRunner.TransactionCallback<?> callback = invocation.getArgument(1);
      return callback.execute(connection);
    });
  }

  @Test
  void openDepositWithdrawsMoneyAndCreatesPendingDeposit() {
    OpenDepositRequest request = openRequest(1L, 5L, new BigDecimal("1000"));
    Account account = account(1L, 7L, 1L, new BigDecimal("2000"));
    DepositType type = depositType(5L, "Копилка", true, new BigDecimal("100"));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(account));
    when(accountDao.withdraw(connection, 1L, new BigDecimal("1000"))).thenReturn(true);
    when(depositDao.createDeposit(eq(connection), eq(7L), eq(5L), eq(false), eq(true), eq(DepositStatus.PENDING), any(), eq(new BigDecimal("1000")))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(null), any(), eq(new BigDecimal("1000")), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("DEPOSIT_OPEN"))).thenReturn(true);

    service.openDeposit(7L, request);

    verify(depositDao).createDeposit(eq(connection), eq(7L), eq(5L), eq(false), eq(true), eq(DepositStatus.PENDING), any(), eq(new BigDecimal("1000")));
  }

  @Test
  void openCapitalDepositForcesProductSettings() {
    OpenDepositRequest request = openRequest(1L, 5L, new BigDecimal("1000"));
    request.setAutoRenewal(true);
    request.setReinvestInterest(false);
    Account account = account(1L, 7L, 1L, new BigDecimal("2000"));
    DepositType type = depositType(5L, "Капитал", false, new BigDecimal("100"));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(account));
    when(accountDao.withdraw(connection, 1L, new BigDecimal("1000"))).thenReturn(true);
    when(depositDao.createDeposit(eq(connection), eq(7L), eq(5L), eq(true), eq(false), eq(DepositStatus.PENDING), any(), eq(new BigDecimal("1000")))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(1L), eq(null), any(), eq(new BigDecimal("1000")), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("DEPOSIT_OPEN"))).thenReturn(true);

    service.openDeposit(7L, request);

    verify(depositDao).createDeposit(eq(connection), eq(7L), eq(5L), eq(true), eq(false), eq(DepositStatus.PENDING), any(), eq(new BigDecimal("1000")));
  }

  @Test
  void topUpDepositRejectsCapitalProduct() {
    Account account = account(1L, 7L, 1L, new BigDecimal("2000"));
    Deposit deposit = deposit(9L, 7L, 5L, DepositStatus.ACTIVE.name(), new BigDecimal("1000"));
    DepositType capital = depositType(5L, "Капитал", false, new BigDecimal("100"));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(account));
    when(depositDao.getDepositByIdForUpdate(connection, 9L)).thenReturn(Optional.of(deposit));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(capital));

    assertThrows(IllegalArgumentException.class, () -> service.topUpDeposit(7L, 1L, 9L, BigDecimal.TEN));
  }

  @Test
  void withdrawFromDepositMovesMoneyToTargetAccount() {
    Account account = account(1L, 7L, 1L, BigDecimal.ZERO);
    Deposit deposit = deposit(9L, 7L, 5L, DepositStatus.ACTIVE.name(), new BigDecimal("1000"));
    DepositType type = depositType(5L, "Копилка", true, new BigDecimal("100"));
    when(depositDao.getDepositByIdForUpdate(connection, 9L)).thenReturn(Optional.of(deposit));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, 1L)).thenReturn(Optional.of(account));
    when(depositDao.withdrawFromDeposit(connection, 9L, new BigDecimal("100"))).thenReturn(true);
    when(accountDao.topUp(connection, 1L, new BigDecimal("100"))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(1L), any(), eq(new BigDecimal("100")), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("DEPOSIT_WITHDRAWAL"))).thenReturn(true);

    service.withdrawFromDeposit(7L, 9L, 1L, new BigDecimal("100"));

    verify(accountDao).topUp(connection, 1L, new BigDecimal("100"));
  }

  @Test
  void accrueInterestTopsUpReinvestedDeposits() {
    Deposit deposit = deposit(9L, 7L, 5L, DepositStatus.ACTIVE.name(), new BigDecimal("1200"));
    DepositType type = depositType(5L, "Стратегия", false, BigDecimal.ZERO);
    type.setRate(new BigDecimal("12"));
    when(depositDao.getDepositsByStatus(DepositStatus.ACTIVE)).thenReturn(List.of(deposit));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(type));
    when(depositDao.topUpDeposit(connection, 9L, new BigDecimal("12.00"))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(null), any(), eq(new BigDecimal("12.00")), eq(1L), eq(BigDecimal.ZERO), anyString(), eq("DEPOSIT_INTEREST"))).thenReturn(true);

    assertEquals(1, service.accrueInterestForActiveDeposits());
  }

  @Test
  void processExpiredDepositsRenewsAutoRenewalDeposits() {
    Deposit deposit = deposit(9L, 7L, 5L, DepositStatus.ACTIVE.name(), new BigDecimal("1200"));
    deposit.setAutoRenewal(true);
    deposit.setStartDate(LocalDate.now().minusMonths(13));
    DepositType type = depositType(5L, "Стратегия", false, BigDecimal.ZERO);
    type.setDuration(12);
    when(depositDao.getDepositsByStatus(DepositStatus.ACTIVE)).thenReturn(List.of(deposit));
    when(depositTypeDao.getDepositTypeById(5L)).thenReturn(Optional.of(type));
    when(depositDao.updateStartDate(eq(connection), eq(9L), any())).thenReturn(true);

    assertEquals(1, service.processExpiredDeposits());
    verify(depositDao).updateStartDate(eq(connection), eq(9L), any());
  }

  private OpenDepositRequest openRequest(Long accountId, Long depositTypeId, BigDecimal amount) {
    OpenDepositRequest request = new OpenDepositRequest();
    request.setSourceAccountId(accountId);
    request.setDepositTypeId(depositTypeId);
    request.setAmount(amount);
    request.setAutoRenewal(true);
    request.setReinvestInterest(false);
    return request;
  }

  private Account account(Long accountId, Long userId, Long currencyId, BigDecimal balance) {
    return new Account(accountId, userId, "4000000000000002", "123", LocalDate.now().plusYears(1), balance, currencyId, AccountStatus.ACTIVE.name(), new BigDecimal("100000"), "Main", true);
  }

  private Deposit deposit(Long depositId, Long userId, Long depositTypeId, String status, BigDecimal amount) {
    return new Deposit(depositId, userId, depositTypeId, true, false, status, LocalDate.now(), amount);
  }

  private DepositType depositType(Long depositTypeId, String name, Boolean withdrawal, BigDecimal minimumAmount) {
    return new DepositType(depositTypeId, name, BigDecimal.TEN, 12, withdrawal, minimumAmount, 1L);
  }
}
