package org.author.demo.dao.deposit;

import org.author.demo.model.Deposit;

import java.util.List;

public interface DepositDao {

  List<Deposit> getPendingDeposits();

  boolean acceptDeposit(Long depositId);

}
