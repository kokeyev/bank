package org.openbank.dao;

import org.openbank.model.Deposit;
import org.openbank.model.status.DepositStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Defines the deposit dao contract.
 */
public interface DepositDao {

  /** Creates a deposit. */
  boolean createDeposit(Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount);

  /** Creates a deposit using an existing transaction. */
  boolean createDeposit(Connection connection, Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount);

  /** Finds a deposit by id. */
  Optional<Deposit> getDepositById(Long depositId);

  /** Finds and locks a deposit by id in an existing transaction. */
  Optional<Deposit> getDepositByIdForUpdate(Connection connection, Long depositId);

  /** Returns deposits for a user. */
  List<Deposit> getDepositsByUserId(Long userId);

  /** Returns deposits with the given status. */
  List<Deposit> getDepositsByStatus(DepositStatus status);

  /** Tops up a deposit. */
  boolean topUpDeposit(Connection connection, Long depositId, BigDecimal amount);

  /** Withdraws money from a deposit. */
  boolean withdrawFromDeposit(Connection connection, Long depositId, BigDecimal amount);

  /** Updates a deposit status. */
  boolean setStatus(Long depositId, DepositStatus status);

  /** Updates a deposit status using an existing transaction. */
  boolean setStatus(Connection connection, Long depositId, DepositStatus status);

  /** Updates a deposit start date. */
  boolean updateStartDate(Connection connection, Long depositId, LocalDate startDate);

  /** Returns pending deposits. */
  List<Deposit> getPendingDeposits();

  /** Accepts a pending deposit. */
  boolean acceptDeposit(Long depositId);

}
