package org.author.demo.dao.deposit;

import org.author.demo.model.Deposit;
import org.author.demo.model.status.DepositStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepositDao {

  boolean createDeposit(Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount);

  boolean createDeposit(Connection connection, Long userId, Long depositTypeId, Boolean reinvestInterest, Boolean autoRenewal, DepositStatus status, LocalDate startDate, BigDecimal currentAmount);

  Optional<Deposit> getDepositById(Long depositId);

  Optional<Deposit> getDepositByIdForUpdate(Connection connection, Long depositId);

  List<Deposit> getDepositsByUserId(Long userId);

  List<Deposit> getDepositsByStatus(DepositStatus status);

  boolean topUpDeposit(Connection connection, Long depositId, BigDecimal amount);

  boolean withdrawFromDeposit(Connection connection, Long depositId, BigDecimal amount);

  boolean setStatus(Long depositId, DepositStatus status);

  boolean setStatus(Connection connection, Long depositId, DepositStatus status);

  boolean updateStartDate(Connection connection, Long depositId, LocalDate startDate);

  List<Deposit> getPendingDeposits();

  boolean acceptDeposit(Long depositId);

}
