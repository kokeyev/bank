package org.openbank.service.impl;

import org.openbank.service.LoanService;
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
import org.openbank.service.DatabaseTransactionRunner;
import org.openbank.service.strategy.loan.LoanProductStrategy;
import org.openbank.service.strategy.loan.LoanProductStrategyResolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LoanServiceImpl implements LoanService {

  private static final String KZT = "KZT";
  private static final String LOAN_DISBURSEMENT = "LOAN_DISBURSEMENT";

  private final LoanDao loanDao;
  private final LoanTypeDao loanTypeDao;
  private final AccountDao accountDao;
  private final CurrencyDao currencyDao;
  private final TransactionDao transactionDao;
  private final DatabaseTransactionRunner transactionRunner;
  private final LoanProductStrategyResolver strategyResolver;
  public LoanServiceImpl(LoanDao loanDao, LoanTypeDao loanTypeDao, AccountDao accountDao, CurrencyDao currencyDao, TransactionDao transactionDao, DatabaseTransactionRunner transactionRunner, LoanProductStrategyResolver strategyResolver) {
    this.loanDao = loanDao;
    this.loanTypeDao = loanTypeDao;
    this.accountDao = accountDao;
    this.currencyDao = currencyDao;
    this.transactionDao = transactionDao;
    this.transactionRunner = transactionRunner;
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
    validateAccountSelected(request.getAccountId());

    Optional<LoanType> loanTypeOptional = getLoanTypeByName(loanTypeName);
    if (loanTypeOptional.isEmpty()) {
      throw new IllegalArgumentException("Тип кредита не найден");
    }
    LoanType loanType = loanTypeOptional.get();
    LoanProductStrategy strategy = strategyResolver.resolve(loanType);

    strategy.validateApplicationAmount(loanType, request.getAmount());
    validateKztAccount(userId, request.getAccountId());

    return loanDao.createPendingLoan(userId, loanType.getLoanTypeId(), request.getAccountId(), request.getAmount());
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
        parentLoan.getAccountId(),
        request.getAmount(),
        request.getRate(),
        request.getDuration(),
        monthlyPayment
    );
  }

  public boolean acceptOffer(Long userId, Long loanId) {
    return transactionRunner.run("Не удалось принять предложение по кредиту", connection -> {
      Optional<Loan> acceptedOffer = loanDao.acceptOffer(connection, userId, loanId);
      if (acceptedOffer.isEmpty()) {
        return false;
      }

      Loan offer = acceptedOffer.get();
      validateAccountSelected(offer.getAccountId());
      Account account = getAccountForUpdate(connection, offer.getAccountId());
      validateKztAccount(userId, account);

      if (!accountDao.topUp(connection, offer.getAccountId(), offer.getRemainingAmount())) {
        throw new IllegalStateException("Не удалось зачислить деньги кредита");
      }

      if (!transactionDao.createNewTransaction(connection, null, offer.getAccountId(), LocalDateTime.now(), offer.getRemainingAmount(), account.getCurrencyId(), BigDecimal.ZERO, "Зачисление кредита #" + offer.getLoanId(), LOAN_DISBURSEMENT)) {
        throw new IllegalStateException("Не удалось сохранить историю зачисления кредита");
      }

      return true;
    });
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

  private void validateAccountSelected(Long accountId) {
    if (accountId == null) {
      throw new IllegalArgumentException("Выберите счет для зачисления кредита");
    }
  }

  private void validateKztAccount(Long userId, Long accountId) {
    Account account = accountDao.getAccountById(accountId)
        .orElseThrow(() -> new IllegalArgumentException("Счет не найден"));
    validateKztAccount(userId, account);
  }

  private void validateKztAccount(Long userId, Account account) {
    if (!account.getUserId().equals(userId)) {
      throw new IllegalArgumentException("Счет не принадлежит текущему пользователю");
    }
    if (!AccountStatus.ACTIVE.name().equals(account.getStatus())) {
      throw new IllegalArgumentException("Кредит можно зачислить только на активный счет");
    }
    String currencyName = currencyDao.getCurrencyNameById(account.getCurrencyId());
    if (!KZT.equals(currencyName)) {
      throw new IllegalArgumentException("Кредит можно зачислить только на счет в KZT");
    }
  }

  private Account getAccountForUpdate(Connection connection, Long accountId) {
    return accountDao.getAccountByIdForUpdate(connection, accountId)
        .orElseThrow(() -> new IllegalArgumentException("Счет не найден"));
  }
}
