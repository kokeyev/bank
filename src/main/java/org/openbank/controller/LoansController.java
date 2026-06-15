package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import org.openbank.dto.LoanApplicationRequest;
import org.openbank.model.User;
import org.openbank.view.BankViewService;
import org.openbank.service.CurrentUserService;
import org.openbank.service.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class LoansController {

  private static final String PURPOSE = "На любые цели";
  private static final String AUTO = "Автокредит";
  private static final String MORTGAGE = "Ипотека";

  private final CurrentUserService currentUserService;
  private final LoanService loanService;
  private final BankViewService bankViewService;

  public LoansController(CurrentUserService currentUserService, LoanService loanService, BankViewService bankViewService) {
    this.currentUserService = currentUserService;
    this.loanService = loanService;
    this.bankViewService = bankViewService;
  }

  @GetMapping("/loans")
  public String loans(HttpSession session, Model model) {
    model.addAttribute("loanTypes", bankViewService.getLoanTypeViews());

    currentUserService.getCurrentUser(session).ifPresent(user -> model.addAttribute("loans", bankViewService.getLoanViews(user.getUserId())));

    return "loans/index";
  }

  @GetMapping("/loans/purpose")
  public String purposeLoan(Model model) {
    addLoanFormModel(model, PURPOSE, new LoanApplicationRequest());
    return "loans/purpose";
  }

  @PostMapping("/loans/purpose")
  public String createPurposeLoan(@ModelAttribute LoanApplicationRequest request, HttpSession session, Model model) {
    return createLoanApplication(PURPOSE, "loans/purpose", request, session, model);
  }

  @GetMapping("/loans/auto")
  public String autoLoan(Model model) {
    addLoanFormModel(model, AUTO, new LoanApplicationRequest());
    return "loans/auto";
  }

  @PostMapping("/loans/auto")
  public String createAutoLoan(@ModelAttribute LoanApplicationRequest request, HttpSession session, Model model) {
    return createLoanApplication(AUTO, "loans/auto", request, session, model);
  }

  @GetMapping("/loans/mortgage")
  public String mortgageLoan(Model model) {
    addLoanFormModel(model, MORTGAGE, new LoanApplicationRequest());
    return "loans/mortgage";
  }

  @PostMapping("/loans/mortgage")
  public String createMortgageLoan(@ModelAttribute LoanApplicationRequest request, HttpSession session, Model model) {
    return createLoanApplication(MORTGAGE, "loans/mortgage", request, session, model);
  }

  @PostMapping("/loans/{loanId}/accept")
  public String acceptLoanOffer(@PathVariable("loanId") Long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    if (loanService.acceptOffer(currentUser.get().getUserId(), loanId)) {
      redirectAttributes.addFlashAttribute("loanSuccess", "Предложение принято. Кредит активирован.");
    } else {
      redirectAttributes.addFlashAttribute("loanError", "Не удалось принять предложение.");
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
      redirectAttributes.addFlashAttribute("loanSuccess", "Предложение отклонено.");
    } else {
      redirectAttributes.addFlashAttribute("loanError", "Не удалось отклонить предложение.");
    }

    return "redirect:/loans";
  }

  private String createLoanApplication(String loanTypeName, String template, LoanApplicationRequest request, HttpSession session, Model model) {
    Optional<User> currentUser = currentUserService.getCurrentUser(session);

    if (currentUser.isEmpty()) {
      return "redirect:/login?loginRequired=true";
    }

    try {
      loanService.createApplication(currentUser.get().getUserId(), loanTypeName, request);
      model.addAttribute("loanSuccess", "Заявка отправлена менеджеру на рассмотрение.");
      addLoanFormModel(model, loanTypeName, new LoanApplicationRequest());
    } catch (IllegalArgumentException e) {
      model.addAttribute("loanError", e.getMessage());
      addLoanFormModel(model, loanTypeName, request);
    }

    return template;
  }

  private void addLoanFormModel(Model model, String loanTypeName, LoanApplicationRequest request) {
    model.addAttribute("loanApplicationRequest", request);
    model.addAttribute("loanType", bankViewService.getLoanTypeView(loanTypeName));
  }
}
