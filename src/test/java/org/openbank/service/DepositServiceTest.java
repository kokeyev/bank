package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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

  private static final Long ACCOUNT_ID = 1L;
  private static final Long USER_ID = 7L;
  private static final Long CURRENCY_ID = 1L;
  private static final Long DEPOSIT_ID = 9L;
  private static final Long DEPOSIT_TYPE_ID = 5L;
  private static final String KOPILKA_PRODUCT_NAME = KopilkaDepositStrategy.PRODUCT_NAME;
  private static final String CAPITAL_PRODUCT_NAME = CapitalDepositStrategy.PRODUCT_NAME;
  private static final String STRATEGY_PRODUCT_NAME = StrategyDepositStrategy.PRODUCT_NAME;
  private static final String CARD_NUMBER = "4000000000000002";
  private static final String CVV = "123";
  private static final String ACCOUNT_NAME = "Main";
  private static final String DEPOSIT_OPEN_TYPE = "DEPOSIT_OPEN";
  private static final String DEPOSIT_WITHDRAWAL_TYPE = "DEPOSIT_WITHDRAWAL";
  private static final String DEPOSIT_INTEREST_TYPE = "DEPOSIT_INTEREST";
  private static final String DEPOSIT_REJECTION_REFUND_TYPE = "DEPOSIT_REJECTION_REFUND";
  private static final BigDecimal OPEN_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal ACCOUNT_BALANCE = new BigDecimal("2000");
  private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("100");
  private static final BigDecimal CURRENT_DEPOSIT_AMOUNT = new BigDecimal("1200");
  private static final BigDecimal INTEREST_RATE = new BigDecimal("12");
  private static final BigDecimal MONTHLY_INTEREST = new BigDecimal("12.00");
  private static final BigDecimal ACCOUNT_LIMIT = new BigDecimal("100000");
  private static final int EXPECTED_UPDATED_COUNT = 1;
  private static final int EXPIRED_DEPOSIT_MONTHS = 13;
  private static final int DEPOSIT_DURATION_MONTHS = 12;

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

  private final MessageService messageService = (code, args) -> code;

  private DepositService service;

  @BeforeEach
  void runCallbacks() {
    DepositProductStrategyResolver strategyResolver = new DepositProductStrategyResolver(List.of(
        new KopilkaDepositStrategy(messageService),
        new StrategyDepositStrategy(messageService),
        new CapitalDepositStrategy(messageService)
    ), messageService);
    service = new DepositServiceImpl(depositDao, depositTypeDao, accountDao, currencyDao, transactionDao, transactionRunner, strategyResolver, messageService);

    when(transactionRunner.run(anyString(), any())).thenAnswer(invocation -> {
      DatabaseTransactionRunner.TransactionCallback<?> callback = invocation.getArgument(1);
      return callback.execute(connection);
    });
  }

  @Test
  void openDepositWithdrawsMoneyAndCreatesPendingDeposit() {
    OpenDepositRequest request = openRequest(ACCOUNT_ID, DEPOSIT_TYPE_ID, OPEN_AMOUNT);
    Account account = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, ACCOUNT_BALANCE);
    DepositType type = depositType(DEPOSIT_TYPE_ID, KOPILKA_PRODUCT_NAME, true, MINIMUM_AMOUNT);
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.withdraw(connection, ACCOUNT_ID, OPEN_AMOUNT)).thenReturn(true);
    when(depositDao.createDeposit(eq(connection), eq(USER_ID), eq(DEPOSIT_TYPE_ID), eq(false), eq(true), eq(DepositStatus.PENDING), any(), eq(OPEN_AMOUNT))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(ACCOUNT_ID), eq(null), any(), eq(OPEN_AMOUNT), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_OPEN_TYPE))).thenReturn(true);

    service.openDeposit(USER_ID, request);

    verify(depositDao).createDeposit(eq(connection), eq(USER_ID), eq(DEPOSIT_TYPE_ID), eq(false), eq(true), eq(DepositStatus.PENDING), any(), eq(OPEN_AMOUNT));
  }

  @Test
  void openCapitalDepositForcesProductSettings() {
    OpenDepositRequest request = openRequest(ACCOUNT_ID, DEPOSIT_TYPE_ID, OPEN_AMOUNT);
    request.setAutoRenewal(true);
    request.setReinvestInterest(false);
    Account account = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, ACCOUNT_BALANCE);
    DepositType type = depositType(DEPOSIT_TYPE_ID, CAPITAL_PRODUCT_NAME, false, MINIMUM_AMOUNT);
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(accountDao.withdraw(connection, ACCOUNT_ID, OPEN_AMOUNT)).thenReturn(true);
    when(depositDao.createDeposit(eq(connection), eq(USER_ID), eq(DEPOSIT_TYPE_ID), eq(true), eq(false), eq(DepositStatus.PENDING), any(), eq(OPEN_AMOUNT))).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(ACCOUNT_ID), eq(null), any(), eq(OPEN_AMOUNT), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_OPEN_TYPE))).thenReturn(true);

    service.openDeposit(USER_ID, request);

    verify(depositDao).createDeposit(eq(connection), eq(USER_ID), eq(DEPOSIT_TYPE_ID), eq(true), eq(false), eq(DepositStatus.PENDING), any(), eq(OPEN_AMOUNT));
  }

  @Test
  void topUpDepositRejectsCapitalProduct() {
    Account account = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, ACCOUNT_BALANCE);
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.ACTIVE.name(), OPEN_AMOUNT);
    DepositType capital = depositType(DEPOSIT_TYPE_ID, CAPITAL_PRODUCT_NAME, false, MINIMUM_AMOUNT);
    when(accountDao.getAccountByIdForUpdate(connection, ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(depositDao.getDepositByIdForUpdate(connection, DEPOSIT_ID)).thenReturn(Optional.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(capital));

    Executable executable = () -> service.topUpDeposit(USER_ID, ACCOUNT_ID, DEPOSIT_ID, BigDecimal.TEN);
    assertThrows(IllegalArgumentException.class, executable);
  }

  @Test
  void withdrawFromDepositMovesMoneyToTargetAccount() {
    Account account = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, BigDecimal.ZERO);
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.ACTIVE.name(), OPEN_AMOUNT);
    DepositType type = depositType(DEPOSIT_TYPE_ID, KOPILKA_PRODUCT_NAME, true, MINIMUM_AMOUNT);
    when(depositDao.getDepositByIdForUpdate(connection, DEPOSIT_ID)).thenReturn(Optional.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(accountDao.getAccountByIdForUpdate(connection, ACCOUNT_ID)).thenReturn(Optional.of(account));
    when(depositDao.withdrawFromDeposit(connection, DEPOSIT_ID, MINIMUM_AMOUNT)).thenReturn(true);
    when(accountDao.topUp(connection, ACCOUNT_ID, MINIMUM_AMOUNT)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(ACCOUNT_ID), any(), eq(MINIMUM_AMOUNT), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_WITHDRAWAL_TYPE))).thenReturn(true);

    service.withdrawFromDeposit(USER_ID, DEPOSIT_ID, ACCOUNT_ID, MINIMUM_AMOUNT);

    verify(accountDao).topUp(connection, ACCOUNT_ID, MINIMUM_AMOUNT);
  }

  @Test
  void rejectDepositReturnsMoneyToActiveAccountAndRejectsApplication() {
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.PENDING.name(), OPEN_AMOUNT);
    DepositType type = depositType(DEPOSIT_TYPE_ID, KOPILKA_PRODUCT_NAME, true, MINIMUM_AMOUNT);
    Account refundAccount = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, ACCOUNT_BALANCE);
    when(depositDao.getDepositByIdForUpdate(connection, DEPOSIT_ID)).thenReturn(Optional.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(accountDao.getFirstActiveAccountByUserIdAndCurrencyIdForUpdate(connection, USER_ID, CURRENCY_ID)).thenReturn(Optional.of(refundAccount));
    when(depositDao.setStatus(connection, DEPOSIT_ID, DepositStatus.REJECTED)).thenReturn(true);
    when(accountDao.topUp(connection, ACCOUNT_ID, OPEN_AMOUNT)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(ACCOUNT_ID), any(), eq(OPEN_AMOUNT), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_REJECTION_REFUND_TYPE))).thenReturn(true);

    service.rejectDeposit(DEPOSIT_ID);

    verify(depositDao).setStatus(connection, DEPOSIT_ID, DepositStatus.REJECTED);
    verify(accountDao).topUp(connection, ACCOUNT_ID, OPEN_AMOUNT);
    verify(transactionDao).createNewTransaction(eq(connection), eq(null), eq(ACCOUNT_ID), any(), eq(OPEN_AMOUNT), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_REJECTION_REFUND_TYPE));
  }

  @Test
  void accrueInterestTopsUpReinvestedDeposits() {
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.ACTIVE.name(), CURRENT_DEPOSIT_AMOUNT);
    DepositType type = depositType(DEPOSIT_TYPE_ID, STRATEGY_PRODUCT_NAME, false, BigDecimal.ZERO);
    type.setRate(INTEREST_RATE);
    when(depositDao.getDepositsByStatus(DepositStatus.ACTIVE)).thenReturn(List.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(depositDao.topUpDeposit(connection, DEPOSIT_ID, MONTHLY_INTEREST)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(null), any(), eq(MONTHLY_INTEREST), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_INTEREST_TYPE))).thenReturn(true);

    assertEquals(EXPECTED_UPDATED_COUNT, service.accrueInterestForActiveDeposits());
  }

  @Test
  void accrueInterestTopsUpActiveAccountWhenInterestIsNotReinvested() {
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.ACTIVE.name(), CURRENT_DEPOSIT_AMOUNT);
    deposit.setReinvestInterest(false);
    DepositType type = depositType(DEPOSIT_TYPE_ID, STRATEGY_PRODUCT_NAME, false, BigDecimal.ZERO);
    type.setRate(INTEREST_RATE);
    Account targetAccount = account(ACCOUNT_ID, USER_ID, CURRENCY_ID, ACCOUNT_BALANCE);
    when(depositDao.getDepositsByStatus(DepositStatus.ACTIVE)).thenReturn(List.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(accountDao.getFirstActiveAccountByUserIdAndCurrencyIdForUpdate(connection, USER_ID, CURRENCY_ID)).thenReturn(Optional.of(targetAccount));
    when(accountDao.topUp(connection, ACCOUNT_ID, MONTHLY_INTEREST)).thenReturn(true);
    when(transactionDao.createNewTransaction(eq(connection), eq(null), eq(ACCOUNT_ID), any(), eq(MONTHLY_INTEREST), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_INTEREST_TYPE))).thenReturn(true);

    assertEquals(EXPECTED_UPDATED_COUNT, service.accrueInterestForActiveDeposits());

    verify(accountDao).topUp(connection, ACCOUNT_ID, MONTHLY_INTEREST);
    verify(transactionDao).createNewTransaction(eq(connection), eq(null), eq(ACCOUNT_ID), any(), eq(MONTHLY_INTEREST), eq(CURRENCY_ID), eq(BigDecimal.ZERO), anyString(), eq(DEPOSIT_INTEREST_TYPE));
  }

  @Test
  void processExpiredDepositsRenewsAutoRenewalDeposits() {
    Deposit deposit = deposit(DEPOSIT_ID, USER_ID, DEPOSIT_TYPE_ID, DepositStatus.ACTIVE.name(), CURRENT_DEPOSIT_AMOUNT);
    deposit.setAutoRenewal(true);
    deposit.setStartDate(LocalDate.now().minusMonths(EXPIRED_DEPOSIT_MONTHS));
    DepositType type = depositType(DEPOSIT_TYPE_ID, STRATEGY_PRODUCT_NAME, false, BigDecimal.ZERO);
    type.setDuration(DEPOSIT_DURATION_MONTHS);
    when(depositDao.getDepositsByStatus(DepositStatus.ACTIVE)).thenReturn(List.of(deposit));
    when(depositTypeDao.getDepositTypeById(DEPOSIT_TYPE_ID)).thenReturn(Optional.of(type));
    when(depositDao.updateStartDate(eq(connection), eq(DEPOSIT_ID), any())).thenReturn(true);

    assertEquals(EXPECTED_UPDATED_COUNT, service.processExpiredDeposits());
    verify(depositDao).updateStartDate(eq(connection), eq(DEPOSIT_ID), any());
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
    return new Account(accountId, userId, CARD_NUMBER, CVV, LocalDate.now().plusYears(1), balance, currencyId, AccountStatus.ACTIVE.name(), ACCOUNT_LIMIT, ACCOUNT_NAME, true);
  }

  private Deposit deposit(Long depositId, Long userId, Long depositTypeId, String status, BigDecimal amount) {
    return new Deposit(depositId, userId, depositTypeId, true, false, status, LocalDate.now(), amount);
  }

  private DepositType depositType(Long depositTypeId, String name, Boolean withdrawal, BigDecimal minimumAmount) {
    return new DepositType(depositTypeId, name, BigDecimal.TEN, DEPOSIT_DURATION_MONTHS, withdrawal, minimumAmount, CURRENCY_ID);
  }
}
