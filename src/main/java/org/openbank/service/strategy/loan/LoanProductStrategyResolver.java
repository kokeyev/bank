package org.openbank.service.strategy.loan;

import org.openbank.model.LoanType;
import org.openbank.service.MessageService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the loan strategy that matches a configured loan product name.
 */
@Component
public class LoanProductStrategyResolver {

  private final Map<String, LoanProductStrategy> strategiesByProductName = new HashMap<>();
  private final MessageService messageService;

  public LoanProductStrategyResolver(List<LoanProductStrategy> strategies, MessageService messageService) {
    this.messageService = messageService;
    for (LoanProductStrategy strategy : strategies) {
      strategiesByProductName.put(strategy.productName(), strategy);
    }
  }

  /**
   * Finds the strategy for the supplied loan type.
   *
   * @param loanType selected loan product
   * @return matching strategy implementation
   * @throws IllegalArgumentException when no strategy is registered for the product name
   */
  public LoanProductStrategy resolve(LoanType loanType) {
    LoanProductStrategy strategy = strategiesByProductName.get(loanType.getName());
    if (strategy == null) {
      throw new IllegalArgumentException(messageService.get("error.loanType.notFound"));
    }

    return strategy;
  }

  /**
   * Returns the default strategy used for generic loan calculations.
   *
   * @return purpose-loan strategy when present, otherwise the first registered strategy
   * @throws IllegalStateException when no loan strategies are registered
   */
  public LoanProductStrategy defaultStrategy() {
    LoanProductStrategy strategy = strategiesByProductName.get(PurposeLoanStrategy.PRODUCT_NAME);
    if (strategy == null && !strategiesByProductName.isEmpty()) {
      return strategiesByProductName.values().iterator().next();
    }
    if (strategy == null) {
      throw new IllegalStateException(messageService.get("loan.validation.strategies.notConfigured"));
    }

    return strategy;
  }
}
