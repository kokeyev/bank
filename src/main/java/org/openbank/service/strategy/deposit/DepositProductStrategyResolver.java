package org.openbank.service.strategy.deposit;

import org.openbank.model.DepositType;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the deposit strategy that matches a configured deposit product name.
 */
@Component
public class DepositProductStrategyResolver {

  private final Map<String, DepositProductStrategy> strategiesByProductName = new HashMap<>();
  private final MessageService messageService;

  public DepositProductStrategyResolver(List<DepositProductStrategy> strategies, MessageService messageService) {
    this.messageService = messageService;
    for (DepositProductStrategy strategy : strategies) {
      strategiesByProductName.put(strategy.productName(), strategy);
    }
  }

  /**
   * Finds the strategy for the supplied deposit type.
   *
   * @param depositType selected deposit terms
   * @return matching strategy implementation
   * @throws IllegalArgumentException when no strategy is registered for the product name
   */
  public DepositProductStrategy resolve(DepositType depositType) {
    DepositProductStrategy strategy = strategiesByProductName.get(depositType.getName());
    if (strategy == null) {
      throw new IllegalArgumentException(messageService.get("error.depositType.notFound"));
    }

    return strategy;
  }
}
