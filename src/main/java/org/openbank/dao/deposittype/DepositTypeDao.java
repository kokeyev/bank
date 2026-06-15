package org.openbank.dao.deposittype;

import org.openbank.model.DepositType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Defines the deposit type dao contract.
 */
public interface DepositTypeDao {

  /** Creates a deposit type. */
  boolean createNewDepositType(String name, BigDecimal rate, Integer duration, Boolean withdrawal, BigDecimal minimumAmount, Long currencyId);

  /** Finds a deposit type by id. */
  Optional<DepositType> getDepositTypeById(Long depositTypeId);

  /** Returns all deposit types. */
  List<DepositType> getAllDepositTypes();

  /** Updates a deposit type rate. */
  boolean changeRateOfDepositType(Long depositTypeId, BigDecimal newRate);

}
