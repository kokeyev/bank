package org.openbank.controller;

import org.openbank.view.BankViewService;
import org.openbank.service.DepositService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ManagerDepositsController {

  private final DepositService depositService;
  private final BankViewService bankViewService;

  public ManagerDepositsController(DepositService depositService, BankViewService bankViewService) {
    this.depositService = depositService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/manager/deposits")
  public String deposits(Model model) {
    model.addAttribute("pendingDeposits", bankViewService.getPendingDepositViews());
    return "manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/approve")
  public String approve(@PathVariable("depositId") Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.approveDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", "Депозит одобрен.");
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", "Не удалось одобрить депозит.");
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/{depositId}/reject")
  public String reject(@PathVariable("depositId") Long depositId, RedirectAttributes redirectAttributes) {
    if (depositService.rejectDeposit(depositId)) {
      redirectAttributes.addFlashAttribute("depositManagerSuccess", "Депозит отклонен.");
    } else {
      redirectAttributes.addFlashAttribute("depositManagerError", "Не удалось отклонить депозит.");
    }

    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/process-expiration")
  public String processExpiration(RedirectAttributes redirectAttributes) {
    int count = depositService.processExpiredDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", "Обработано депозитов: " + count + ".");
    return "redirect:/manager/deposits";
  }

  @PostMapping("/manager/deposits/accrue-interest")
  public String accrueInterest(RedirectAttributes redirectAttributes) {
    int count = depositService.accrueInterestForActiveDeposits();
    redirectAttributes.addFlashAttribute("depositManagerSuccess", "Начислено вознаграждение по депозитам: " + count + ".");
    return "redirect:/manager/deposits";
  }
}
