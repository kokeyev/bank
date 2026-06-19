package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.loan.LoanDao;
import org.openbank.dao.loantype.LoanTypeDao;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.LoanStatus;
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
    service = new LoanService(loanDao, loanTypeDao, strategyResolver);
  }

  @Test
  void createApplicationValidatesAmountRangeAndDelegates() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(new BigDecimal("500000"));
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));

    service.createApplication(7L, "Ипотека", request);

    verify(loanDao).createPendingLoan(7L, 2L, new BigDecimal("500000"));
  }

  @Test
  void createApplicationRejectsAmountBelowMinimum() {
    LoanApplicationRequest request = new LoanApplicationRequest();
    request.setAmount(new BigDecimal("100"));
    when(loanTypeDao.getAllLoanTypes()).thenReturn(List.of(loanType()));

    assertThrows(IllegalArgumentException.class, () -> service.createApplication(7L, "Ипотека", request));
  }

  @Test
  void createOfferCalculatesMonthlyPaymentWhenMissing() {
    Loan parentLoan = loan(10L, 7L, 2L, new BigDecimal("500000"), LoanStatus.PENDING.name());
    LoanOfferRequest request = new LoanOfferRequest();
    request.setAmount(new BigDecimal("500000"));
    request.setRate(new BigDecimal("12"));
    request.setDuration(12);
    when(loanDao.getLoanById(10L)).thenReturn(Optional.of(parentLoan));
    when(loanTypeDao.getLoanTypeById(2L)).thenReturn(Optional.of(loanType()));

    service.createOffer(10L, request);

    verify(loanDao).createOffer(
        10L,
        7L,
        2L,
        new BigDecimal("500000"),
        new BigDecimal("12"),
        12,
        new BigDecimal("44424.39")
    );
  }

  @Test
  void createOfferRejectsDurationAboveProductLimit() {
    Loan parentLoan = loan(10L, 7L, 2L, new BigDecimal("500000"), LoanStatus.PENDING.name());
    LoanOfferRequest request = new LoanOfferRequest();
    request.setAmount(new BigDecimal("500000"));
    request.setRate(new BigDecimal("12"));
    request.setDuration(13);
    when(loanDao.getLoanById(10L)).thenReturn(Optional.of(parentLoan));
    when(loanTypeDao.getLoanTypeById(2L)).thenReturn(Optional.of(loanType()));

    assertThrows(IllegalArgumentException.class, () -> service.createOffer(10L, request));
  }

  @Test
  void calculateLatePenaltyReturnsOnePercentOfOverdueAmount() {
    Loan loan = loan(10L, 7L, 2L, new BigDecimal("1000"), LoanStatus.ACTIVE.name());
    loan.setStartDate(LocalDate.now().minusMonths(2));
    loan.setDuration(4);
    loan.setMonthlyPayment(new BigDecimal("100"));
    when(loanTypeDao.getLoanTypeById(2L)).thenReturn(Optional.of(loanType()));

    assertEquals(new BigDecimal("8.00"), service.calculateLatePenalty(loan));
  }

  @Test
  void getPaymentDueDatesBuildsScheduleFromStartDate() {
    Loan loan = loan(10L, 7L, 2L, BigDecimal.TEN, LoanStatus.ACTIVE.name());
    loan.setStartDate(LocalDate.of(2026, 1, 15));
    loan.setDuration(3);
    when(loanTypeDao.getLoanTypeById(2L)).thenReturn(Optional.of(loanType()));

    assertEquals(
        List.of(LocalDate.of(2026, 2, 15), LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 15)),
        service.getPaymentDueDates(loan)
    );
  }

  private LoanType loanType() {
    return new LoanType(2L, "Ипотека", new BigDecimal("12"), 12, new BigDecimal("1000"), new BigDecimal("1000000"), 1L);
  }

  private Loan loan(Long loanId, Long userId, Long loanTypeId, BigDecimal amount, String status) {
    return new Loan(loanId, userId, loanTypeId, null, amount, null, null, status, null, null);
  }
}
