package org.openbank.service.impl;

import org.openbank.service.LoanService;
import org.openbank.dao.LoanDao;
import org.openbank.dao.LoanTypeDao;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.model.Loan;
import org.openbank.model.LoanType;
import org.openbank.model.status.LoanStatus;
import org.openbank.service.strategy.loan.LoanProductStrategy;
import org.openbank.service.strategy.loan.LoanProductStrategyResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class LoanServiceImpl implements LoanService {

  private final LoanDao loanDao;
  private final LoanTypeDao loanTypeDao;
  private final LoanProductStrategyResolver strategyResolver;
  public LoanServiceImpl(LoanDao loanDao, LoanTypeDao loanTypeDao, LoanProductStrategyResolver strategyResolver) {
    this.loanDao = loanDao;
    this.loanTypeDao = loanTypeDao;
    this.strategyResolver = strategyResolver;
  }

  public List<LoanType> getAllLoanTypes() {
    return loanTypeDao.getAllLoanTypes();
  }

  public Optional<LoanType> getLoanTypeByName(String name) {
    for (LoanType loanType : loanTypeDao.getAllLoanTypes()) {
      if (loanType.getName().equals(name)) {
        return Optional.of(loanType);
      }
    }
    return Optional.empty();
  }

  public Optional<LoanType> getLoanTypeById(Long loanTypeId) {
    return loanTypeDao.getLoanTypeById(loanTypeId);
  }

  public List<Loan> getLoansByUserId(Long userId) {
    return loanDao.getLoansByUserId(userId);
  }

  public List<Loan> getActiveLoansByUserId(Long userId) {
    return loanDao.getActiveLoansByUserId(userId);
  }

  public List<Loan> getPendingLoans() {
    return loanDao.getPendingLoans();
  }

  public boolean createApplication(Long userId, String loanTypeName, LoanApplicationRequest request) {
    validatePositive(request.getAmount(), "Введите сумму кредита");

    Optional<LoanType> loanTypeOptional = getLoanTypeByName(loanTypeName);
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalArgumentException("Тип кредита не найден");
    }
    LoanType loanType = loanTypeOptional.get();
    LoanProductStrategy strategy = strategyResolver.resolve(loanType);

    strategy.validateApplicationAmount(loanType, request.getAmount());

    return loanDao.createPendingLoan(userId, loanType.getLoanTypeId(), request.getAmount());
  }

  public boolean createOffer(Long parentLoanId, LoanOfferRequest request) {
    validatePositive(request.getAmount(), "Введите сумму предложения");
    validatePositive(request.getRate(), "Введите процентную ставку");

    if (request.getDuration() == null || request.getDuration() <= 0) {
      throw new IllegalArgumentException("Введите срок кредита");
    }

    Optional<Loan> loanOptional = loanDao.getLoanById(parentLoanId);
    if (loanOptional.isEmpty()) {
      throw new IllegalArgumentException("Заявка не найдена");
    }
    Loan parentLoan = loanOptional.get();

    if (!LoanStatus.PENDING.name().equals(parentLoan.getStatus())) {
      throw new IllegalArgumentException("Предложения можно создавать только для заявок в статусе PENDING");
    }

    Optional<LoanType> loanTypeOptional = loanTypeDao.getLoanTypeById(parentLoan.getLoanTypeId());
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalArgumentException("Тип кредита не найден");
    }
    LoanType loanType = loanTypeOptional.get();
    LoanProductStrategy strategy = strategyResolver.resolve(loanType);

    strategy.validateOffer(loanType, request.getAmount(), request.getDuration());

    BigDecimal monthlyPayment = request.getMonthlyPayment();
    if (monthlyPayment == null || monthlyPayment.compareTo(BigDecimal.ZERO) <= 0) {
      monthlyPayment = strategy.calculateMonthlyPayment(request.getAmount(), request.getRate(), request.getDuration());
    }

    return loanDao.createOffer(
        parentLoan.getLoanId(),
        parentLoan.getUserId(),
        parentLoan.getLoanTypeId(),
        request.getAmount(),
        request.getRate(),
        request.getDuration(),
        monthlyPayment
    );
  }

  public boolean acceptOffer(Long userId, Long loanId) {
    return loanDao.acceptOffer(userId, loanId);
  }

  public boolean rejectOffer(Long userId, Long loanId) {
    return loanDao.refuseOffer(userId, loanId);
  }

  public boolean rejectApplication(Long loanId) {
    return loanDao.rejectPendingLoan(loanId);
  }

  public BigDecimal calculateLatePenalty(Loan loan) {
    if (loan == null || loan.getLoanTypeId() == null) {
      return BigDecimal.ZERO;
    }

    return loanTypeDao.getLoanTypeById(loan.getLoanTypeId())
        .map(strategyResolver::resolve)
        .map(strategy -> strategy.calculateLatePenalty(loan))
        .orElse(BigDecimal.ZERO);
  }

  public List<LocalDate> getPaymentDueDates(Loan loan) {
    if (loan == null || loan.getLoanTypeId() == null) {
      return List.of();
    }

    return loanTypeDao.getLoanTypeById(loan.getLoanTypeId())
        .map(strategyResolver::resolve)
        .map(strategy -> strategy.getPaymentDueDates(loan))
        .orElse(List.of());
  }

  public BigDecimal calculateMonthlyPayment(BigDecimal amount, BigDecimal annualRate, Integer duration) {
    return strategyResolver.defaultStrategy().calculateMonthlyPayment(amount, annualRate, duration);
  }

  private void validatePositive(BigDecimal amount, String message) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(message);
    }
  }
}
