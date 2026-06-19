package org.openbank.controller;

import jakarta.validation.Valid;
import org.openbank.dto.LoanOfferRequest;
import org.openbank.service.MessageService;
import org.openbank.view.BankViewService;
import org.openbank.service.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
public class ManagerLoansController {

  private static final int PAGE_SIZE = 5;

  private final LoanService loanService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public ManagerLoansController(LoanService loanService, BankViewService bankViewService, MessageService messageService) {
    this.loanService = loanService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/manager/loans")
  public String pendingLoans(@RequestParam(value = "page", defaultValue = "1") int page, Model model) {
    addPendingLoanModel(model, new LoanOfferRequest(), page);
    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/offers")
  public String createOffer(@PathVariable("loanId") Long loanId, @RequestParam(value = "page", defaultValue = "1") int page, @Valid @ModelAttribute LoanOfferRequest request, BindingResult bindingResult, Model model) {

    if (bindingResult.hasErrors()) {
      List<String> errorMessages = new ArrayList<>();
      for (FieldError error : bindingResult.getFieldErrors()) {
        errorMessages.add(error.getDefaultMessage());
      }
      model.addAttribute("managerLoanErrors", errorMessages);
      addPendingLoanModel(model, request, page);
      return "manager/loans";
    }


    try {
      loanService.createOffer(loanId, request);
      model.addAttribute("managerLoanSuccess", messageService.get("manager.loans.offer.success"));
      addPendingLoanModel(model, new LoanOfferRequest(), page);
    } catch (IllegalArgumentException e) {
      model.addAttribute("managerLoanError", e.getMessage());
      addPendingLoanModel(model, request, page);
    }

    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/reject")
  public String rejectApplication(@PathVariable("loanId") Long loanId, RedirectAttributes redirectAttributes) {
    if (loanService.rejectApplication(loanId)) {
      redirectAttributes.addFlashAttribute("managerLoanSuccess", messageService.get("manager.loans.reject.success"));
    } else {
      redirectAttributes.addFlashAttribute("managerLoanError", messageService.get("manager.loans.reject.error"));
    }

    return "redirect:/manager/loans";
  }

  private void addPendingLoanModel(Model model, LoanOfferRequest request, int page) {
    model.addAttribute("pendingLoansPage", bankViewService.getPendingLoanViewsPage(page, PAGE_SIZE));
    model.addAttribute("loanOfferRequest", request);
  }
}
