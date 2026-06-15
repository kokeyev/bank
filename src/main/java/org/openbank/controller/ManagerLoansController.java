package org.openbank.controller;

import jakarta.validation.Valid;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.view.BankViewService;
import org.openbank.service.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ManagerLoansController {

  private final LoanService loanService;
  private final BankViewService bankViewService;

  public ManagerLoansController(LoanService loanService, BankViewService bankViewService) {
    this.loanService = loanService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/manager/loans")
  public String pendingLoans(Model model) {
    addPendingLoanModel(model, new LoanOfferRequest());
    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/offers")
  public String createOffer(@PathVariable("loanId") Long loanId, @Valid @ModelAttribute LoanOfferRequest request, BindingResult bindingResult, Model model) {
    if (bindingResult.hasErrors()) {
      model.addAttribute("managerLoanErrors", bindingResult.getFieldErrors().stream()
          .map(error -> error.getDefaultMessage())
          .toList());
      addPendingLoanModel(model, request);
      return "manager/loans";
    }

    try {
      loanService.createOffer(loanId, request);
      model.addAttribute("managerLoanSuccess", "Предложение создано.");
      addPendingLoanModel(model, new LoanOfferRequest());
    } catch (IllegalArgumentException e) {
      model.addAttribute("managerLoanError", e.getMessage());
      addPendingLoanModel(model, request);
    }

    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/reject")
  public String rejectApplication(@PathVariable("loanId") Long loanId, RedirectAttributes redirectAttributes) {
    if (loanService.rejectApplication(loanId)) {
      redirectAttributes.addFlashAttribute("managerLoanSuccess", "Заявка отклонена.");
    } else {
      redirectAttributes.addFlashAttribute("managerLoanError", "Не удалось отклонить заявку.");
    }

    return "redirect:/manager/loans";
  }

  private void addPendingLoanModel(Model model, LoanOfferRequest request) {
    model.addAttribute("pendingLoans", bankViewService.getPendingLoanViews());
    model.addAttribute("loanOfferRequest", request);
  }
}
