package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.AccountDao;
import org.openbank.dao.CurrencyDao;
import org.openbank.dao.LoanDao;
import org.openbank.dao.LoanTypeDao;
import org.openbank.dao.TransactionDao;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Account;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.AccountStatus;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.impl.LoanServiceImpl;
import org.openbank.service.strategy.loan.AutoLoanStrategy;
import org.openbank.service.strategy.loan.LoanProductStrategyResolver;
import org.openbank.service.strategy.loan.MortgageLoanStrategy;
import org.openbank.service.strategy.loan.PurposeLoanStrategy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  private static final Long LOAN_ID = 10L;
  private static final Long USER_ID = 7L;
  private static final Long LOAN_TYPE_ID = 2L;
  private static final Long CURRENCY_ID = 1L;
  private static final Long ACCOUNT_ID = 3L;
  private static final String LOAN_TYPE_NAME = MortgageLoanStrategy.PRODUCT_NAME;
  private static final String KZT = "KZT";
  private static final BigDecimal APPLICATION_AMOUNT = new BigDecimal("500000");
  private static final BigDecimal BELOW_MINIMUM_AMOUNT = new BigDecimal("100");
  private static final BigDecimal LOAN_RATE = new BigDecimal("12");
  private static final BigDecimal MINIMUM_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal MAXIMUM_AMOUNT = new BigDecimal("1000000");
  private static final BigDecimal EXPECTED_MONTHLY_PAYMENT = new BigDecimal("44424.39");
  private static final BigDecimal OVERDUE_AMOUNT = new BigDecimal("1000");
  private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("100");
  private static final BigDecimal EXPECTED_LATE_PENALTY = new BigDecimal("8.00");
  private static final int LOAN_DURATION = 12;
  private static final int INVALID_LOAN_DURATION = 13;
  private static final int OVERDUE_DURATION = 4;
  private static final int OVERDUE_MONTHS = 2;
  private static final int SCHEDULE_DURATION = 3;
  private static final LocalDate SCHEDULE_START_DATE = LocalDate.of(2026, 1, 15);
  private static final List<LocalDate> EXPECTED_DUE_DATES = List.of(
      LocalDate.of(2026, 2, 15),
      LocalDate.of(2026, 3, 15),
      LocalDate.of(2026, 4, 15)
  );

  @Mock
  private LoanDao loanDao;

  @Mock
  private LoanTypeDao loanTypeDao;

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

  private LoanService service;

  @BeforeEach
  void setUp() {
    LoanProductStrategyResolver strategyResolver = new LoanProductStrategyResolver(List.of(new PurposeLoanStrategy(messageService), new AutoLoanStrategy(messageService), new MortgageLoanStrategy(messageService)), messageService);
    service = new LoanServiceImpl(loanDao, loanTypeDao, accountDao, currencyDao, transactionDao, transactionRunner, strategyResolver, messageService);
  }

  @Test
  void createApplicationValidatesAmountRangeAndDelegates() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(APPLICATION_AMOUNT);
    request.setAccountId(ACCOUNT_ID);
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));
    when(accountDao.getAccountById(ACCOUNT_ID)).thenReturn(Optional.of(account()));
    when(currencyDao.getCurrencyNameById(CURRENCY_ID)).thenReturn(KZT);

    service.createApplication(USER_ID, LOAN_TYPE_NAME, request);

    verify(loanDao).createPendingLoan(USER_ID, LOAN_TYPE_ID, ACCOUNT_ID, APPLICATION_AMOUNT);
  }

  @Test
  void createApplicationRejectsAmountBelowMinimum() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(BELOW_MINIMUM_AMOUNT);
    request.setAccountId(ACCOUNT_ID);
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));

    Executable executable = () -> service.createApplication(USER_ID, LOAN_TYPE_NAME, request);
    assertThrows(IllegalArgumentException.class, executable);
  }

  @Test
  void createOfferCalculatesMonthlyPaymentWhenMissing() {
    Loan parentLoan = loan(LOAN_ID, USER_ID, LOAN_TYPE_ID, APPLICATION_AMOUNT, LoanStatus.PENDING.name());
    LoanOfferRequest request = new LoanOfferRequest();
    request.setAmount(APPLICATION_AMOUNT);
    request.setRate(LOAN_RATE);
    request.setDuration(LOAN_DURATION);
    when(loanDao.getLoanById(LOAN_ID)).thenReturn(Optional.of(parentLoan));
    when(loanTypeDao.getLoanTypeById(LOAN_TYPE_ID)).thenReturn(Optional.of(loanType()));

    service.createOffer(LOAN_ID, request);

    verify(loanDao).createOffer(
        LOAN_ID,
        USER_ID,
        LOAN_TYPE_ID,
        ACCOUNT_ID,
        APPLICATION_AMOUNT,
        LOAN_RATE,
        LOAN_DURATION,
        EXPECTED_MONTHLY_PAYMENT
    );
  }

  @Test
  void createOfferRejectsDurationAboveProductLimit() {
    Loan parentLoan = loan(LOAN_ID, USER_ID, LOAN_TYPE_ID, APPLICATION_AMOUNT, LoanStatus.PENDING.name());
    LoanOfferRequest request = new LoanOfferRequest();
    request.setAmount(APPLICATION_AMOUNT);
    request.setRate(LOAN_RATE);
    request.setDuration(INVALID_LOAN_DURATION);
    when(loanDao.getLoanById(LOAN_ID)).thenReturn(Optional.of(parentLoan));
    when(loanTypeDao.getLoanTypeById(LOAN_TYPE_ID)).thenReturn(Optional.of(loanType()));

    Executable executable = () -> service.createOffer(LOAN_ID, request);
    assertThrows(IllegalArgumentException.class, executable);
  }

  @Test
  void acceptOfferCreditsSelectedKztAccountAndCreatesTransaction() throws Exception {
    Loan offer = loan(LOAN_ID, USER_ID, LOAN_TYPE_ID, APPLICATION_AMOUNT, LoanStatus.OFFERED.name());
    when(transactionRunner.run(anyString(), any())).thenAnswer(invocation -> {
      DatabaseTransactionRunner.TransactionCallback<Boolean> callback = invocation.getArgument(1);
      return callback.execute(connection);
    });
    when(loanDao.acceptOffer(connection, USER_ID, LOAN_ID)).thenReturn(Optional.of(offer));
    when(accountDao.getAccountByIdForUpdate(connection, ACCOUNT_ID)).thenReturn(Optional.of(account()));
    when(currencyDao.getCurrencyNameById(CURRENCY_ID)).thenReturn(KZT);
    when(accountDao.topUp(connection, ACCOUNT_ID, APPLICATION_AMOUNT)).thenReturn(true);
    when(transactionDao.createNewTransaction(any(), any(), any(), any(), any(), any(), any(), anyString(), anyString())).thenReturn(true);

    assertTrue(service.acceptOffer(USER_ID, LOAN_ID));

    verify(accountDao).topUp(connection, ACCOUNT_ID, APPLICATION_AMOUNT);
    verify(transactionDao).createNewTransaction(any(), any(), any(), any(), any(), any(), any(), anyString(), anyString());
  }

  @Test
  void calculateLatePenaltyReturnsOnePercentOfOverdueAmount() {
    Loan loan = loan(LOAN_ID, USER_ID, LOAN_TYPE_ID, OVERDUE_AMOUNT, LoanStatus.ACTIVE.name());
    loan.setStartDate(LocalDate.now().minusMonths(OVERDUE_MONTHS));
    loan.setDuration(OVERDUE_DURATION);
    loan.setMonthlyPayment(MONTHLY_PAYMENT);
    when(loanTypeDao.getLoanTypeById(LOAN_TYPE_ID)).thenReturn(Optional.of(loanType()));

    assertEquals(EXPECTED_LATE_PENALTY, service.calculateLatePenalty(loan));
  }

  @Test
  void getPaymentDueDatesBuildsScheduleFromStartDate() {
    Loan loan = loan(LOAN_ID, USER_ID, LOAN_TYPE_ID, BigDecimal.TEN, LoanStatus.ACTIVE.name());
    loan.setStartDate(SCHEDULE_START_DATE);
    loan.setDuration(SCHEDULE_DURATION);
    when(loanTypeDao.getLoanTypeById(LOAN_TYPE_ID)).thenReturn(Optional.of(loanType()));

    assertEquals(EXPECTED_DUE_DATES, service.getPaymentDueDates(loan));
  }

  private LoanType loanType() {
    return new LoanType(LOAN_TYPE_ID, LOAN_TYPE_NAME, LOAN_RATE, LOAN_DURATION, MINIMUM_AMOUNT, MAXIMUM_AMOUNT, CURRENCY_ID);
  }

  private Loan loan(Long loanId, Long userId, Long loanTypeId, BigDecimal amount, String status) {
    return new Loan(loanId, userId, loanTypeId, null, ACCOUNT_ID, amount, null, null, status, null, null);
  }

  private Account account() {
    return new Account(ACCOUNT_ID, USER_ID, null, null, null, BigDecimal.ZERO, CURRENCY_ID, AccountStatus.ACTIVE.name(), BigDecimal.ZERO, "KZT", false);
  }
}
