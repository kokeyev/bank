package org.author.demo.dao.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionDao {

  boolean createNewTransaction(Long senderAccountId, Long receiverAccountId, LocalDateTime transactionDate, BigDecimal amount, Long currencyId, BigDecimal fee, String message, String transactionType);

}
