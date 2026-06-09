package org.author.demo.dao.account;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountDao {

  boolean createNewAccount(Long userId, String cardNumber, LocalDate expiryDate, BigDecimal balance, Long currencyId, String status, BigDecimal transactionLimit, String name);

  boolean setStatusToAccount(Long accountId, String status);

  boolean withdraw(Long accountId, BigDecimal amountToWithdraw);

  boolean topUp(Long accountId, BigDecimal amountToTopUp);

}
