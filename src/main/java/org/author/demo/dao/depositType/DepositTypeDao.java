package org.author.demo.dao.depositType;

import org.author.demo.model.DepositType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DepositTypeDao {

  boolean createNewDepositType(String name, BigDecimal rate, Integer duration, Boolean withdrawal, BigDecimal minimumAmount, Long currencyId);

  Optional<DepositType> getDepositTypeById(Long depositTypeId);

  List<DepositType> getAllDepositTypes();

  boolean changeRateOfDepositType(Long depositTypeId, BigDecimal newRate);

}
