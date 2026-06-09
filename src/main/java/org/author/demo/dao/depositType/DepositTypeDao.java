package org.author.demo.dao.depositType;

import java.math.BigDecimal;

public interface DepositTypeDao {

  boolean createNewDepositType(String name, BigDecimal rate, Integer duration, Boolean withdrawal, BigDecimal minimumAmount, Long currencyId);

  boolean changeRateOfDepositType(Long depositTypeId, BigDecimal newRate);

}
