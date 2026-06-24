package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.LoanService;
import org.openbank.service.MessageService;
import org.openbank.service.strategy.loan.AutoLoanStrategy;
import org.openbank.service.strategy.loan.MortgageLoanStrategy;
import org.openbank.service.strategy.loan.PurposeLoanStrategy;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoansController {

  private static final String PURPOSE = PurposeLoanStrategy.PRODUCT_NAME;
  private static final String AUTO = AutoLoanStrategy.PRODUCT_NAME;
  private static final String MORTGAGE = MortgageLoanStrategy.PRODUCT_NAME;
  private static final int LOAN_PAGE_SIZE = 4;

  private final CurrentUserService currentUserService;
  private final LoanService loanService;
  private final BankViewService bankViewService;
  private final MessageService messageService;

  public LoansController(CurrentUserService currentUserService, LoanService loanService, BankViewService bankViewService, MessageService messageService) {
    this.currentUserService = currentUserService;
    this.loanService = loanService;
    this.bankViewService = bankViewService;
    this.messageService = messageService;
  }

  @GetMapping("/loans")
  public String loans(@RequestParam(value = "page", defaultValue = "1") int page, HttpSession session, Model model) {
    model.addAttribute("loanTypes", bankViewService.getLoanTypeViews());

    currentUserService.getCurrentUser(session).ifPresent(user -> model.addAttribute("loansPage", bankViewService.getLoanViewsPage(user.getUserId(), page, LOAN_PAGE_SIZE)));

    return "loans/index";
  }

  @GetMapping("/loans/purpose")
  public String purposeLoan(HttpSession session, Model model) {
    addLoanFormModel(model, PURPOSE, new LoanApplicationRequest(), session);
    return "loans/purpose";
  }

  @PostMapping("/loans/purpose")
  public String createPurposeLoan(@Valid @ModelAttribute LoanApplicationRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    return createLoanApplication(PURPOSE, "loans/purpose", request, bindingResult, session, model);
  }

  @GetMapping("/loans/auto")
  public String autoLoan(HttpSession session, Model model) {
    addLoanFormModel(model, AUTO, new LoanApplicationRequest(), session);
    return "loans/auto";
  }

  @PostMapping("/loans/auto")
  public String createAutoLoan(@Valid @ModelAttribute LoanApplicationRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    return createLoanApplication(AUTO, "loans/auto", request, bindingResult, session, model);
  }

  @GetMapping("/loans/mortgage")
  public String mortgageLoan(HttpSession session, Model model) {
    addLoanFormModel(model, MORTGAGE, new LoanApplicationRequest(), session);
    return "loans/mortgage";
  }

  @PostMapping("/loans/mortgage")
  public String createMortgageLoan(@Valid @ModelAttribute LoanApplicationRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    return createLoanApplication(MORTGAGE, "loans/mortgage", request, bindingResult, session, model);
  }

  @PostMapping("/loans/{loanId}/accept")
  public String acceptLoanOffer(@PathVariable("loanId") Long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (loanService.acceptOffer(currentUser.get().getUserId(), loanId)) {
      redirectAttributes.addFlashAttribute("loanSuccess", messageService.get("loans.offer.accept.success"));
    } else {
      redirectAttributes.addFlashAttribute("loanError", messageService.get("loans.offer.accept.error"));
    }

    return "redirect:/loans";
  }

  @PostMapping("/loans/{loanId}/reject")
  public String rejectLoanOffer(@PathVariable("loanId") Long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (loanService.rejectOffer(currentUser.get().getUserId(), loanId)) {
      redirectAttributes.addFlashAttribute("loanSuccess", messageService.get("loans.offer.reject.success"));
    } else {
      redirectAttributes.addFlashAttribute("loanError", messageService.get("loans.offer.reject.error"));
    }

    return "redirect:/loans";
  }

  private String createLoanApplication(String loanTypeName, String template, LoanApplicationRequest request, BindingResult bindingResult, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (bindingResult.hasErrors()) {
      addLoanFormModel(model, loanTypeName, request, session);
      return template;
    }

    try {
      loanService.createApplication(currentUser.get().getUserId(), loanTypeName, request);
      model.addAttribute("loanSuccess", messageService.get("loans.request.success"));
      addLoanFormModel(model, loanTypeName, new LoanApplicationRequest(), session);
    } catch (IllegalArgumentException e) {
      model.addAttribute("loanError", e.getMessage());
      addLoanFormModel(model, loanTypeName, request, session);
    }

    return template;
  }

  private void addLoanFormModel(Model model, String loanTypeName, LoanApplicationRequest request, HttpSession session) {
    model.addAttribute("loanApplicationRequest", request);
    model.addAttribute("loanType", bankViewService.getLoanTypeView(loanTypeName));
    currentUserService.getCurrentUser(session)
        .ifPresent(user -> model.addAttribute("kztAccountOptions", bankViewService.getKztAccountOptions(user.getUserId())));
  }
}
