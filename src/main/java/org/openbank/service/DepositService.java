package org.openbank.service;

import org.openbank.dto.OpenDepositRequest;
import org.openbank.model.Deposit;
import org.openbank.model.DepositType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Defines deposit product and lifecycle operations.
 */
public interface DepositService {

  /** Returns deposit terms for a product name. */
  List<DepositType> getDepositTypesByProduct(String productName);

  /** Returns deposits owned by a user. */
  List<Deposit> getDepositsByUserId(Long userId);

  /** Finds a deposit type by id. */
  Optional<DepositType> getDepositTypeById(Long depositTypeId);

  /** Resolves a currency id to its display name. */
  String getCurrencyNameById(Long currencyId);

  /** Opens a pending deposit from a source account. */
  boolean openDeposit(Long userId, OpenDepositRequest request);

  /** Moves money from an account into a deposit. */
  boolean topUpDeposit(Long userId, Long sourceAccountId, Long depositId, BigDecimal amount);

  /** Withdraws part of a deposit to an account. */
  boolean withdrawFromDeposit(Long userId, Long depositId, Long targetAccountId, BigDecimal amount);

  /** Returns deposits waiting for manager approval. */
  List<Deposit> getPendingDeposits();

  /** Approves a pending deposit. */
  boolean approveDeposit(Long depositId);

  /** Rejects a pending deposit. */
  boolean rejectDeposit(Long depositId);

  /** Accrues monthly interest for active deposits. */
  int accrueInterestForActiveDeposits();

  /** Processes active deposits whose maturity date passed. */
  int processExpiredDeposits();

  /** Checks whether a deposit can be topped up. */
  boolean canTopUpDeposit(Deposit deposit, DepositType depositType);

  /** Checks whether a deposit product allows withdrawals. */
  boolean canWithdrawDeposit(DepositType depositType);
}
