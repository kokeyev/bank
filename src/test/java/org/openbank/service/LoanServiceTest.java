package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.LoanDao;
import org.openbank.dao.LoanTypeDao;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.impl.LoanServiceImpl;
import org.openbank.service.strategy.loan.AutoLoanStrategy;
import org.openbank.service.strategy.loan.LoanProductStrategyResolver;
import org.openbank.service.strategy.loan.MortgageLoanStrategy;
import org.openbank.service.strategy.loan.PurposeLoanStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

  private static final Long LOAN_ID = 10L;
  private static final Long USER_ID = 7L;
  private static final Long LOAN_TYPE_ID = 2L;
  private static final Long CURRENCY_ID = 1L;
  private static final String LOAN_TYPE_NAME = "Ипотека";
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

  private LoanService service;

  @BeforeEach
  void setUp() {
    LoanProductStrategyResolver strategyResolver = new LoanProductStrategyResolver(List.of(
        new PurposeLoanStrategy(),
        new AutoLoanStrategy(),
        new MortgageLoanStrategy()
    ));
    service = new LoanServiceImpl(loanDao, loanTypeDao, strategyResolver);
  }

  @Test
  void createApplicationValidatesAmountRangeAndDelegates() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(APPLICATION_AMOUNT);
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));

    service.createApplication(USER_ID, LOAN_TYPE_NAME, request);

    verify(loanDao).createPendingLoan(USER_ID, LOAN_TYPE_ID, APPLICATION_AMOUNT);
  }

  @Test
  void createApplicationRejectsAmountBelowMinimum() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(BELOW_MINIMUM_AMOUNT);
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));

    assertThrows(IllegalArgumentException.class, () -> service.createApplication(USER_ID, LOAN_TYPE_NAME, request));
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

    assertThrows(IllegalArgumentException.class, () -> service.createOffer(LOAN_ID, request));
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
    return new Loan(loanId, userId, loanTypeId, null, amount, null, null, status, null, null);
  }
}
