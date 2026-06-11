package org.author.demo.controllers;

import org.author.demo.dto.LoanOfferRequest;
import org.author.demo.dto.LoanView;
import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.services.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

@Controller
public class ManagerLoansController {

  private final LoanService loanService;

  public ManagerLoansController(LoanService loanService) {
    this.loanService = loanService;
  }

  @GetMapping("/manager/loans")
  public String pendingLoans(Model model) {
    model.addAttribute("pendingLoans", getPendingLoanViews());
    model.addAttribute("loanOfferRequest", new LoanOfferRequest());
    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/offers")
  public String createOffer(@PathVariable Long loanId, @ModelAttribute LoanOfferRequest request, Model model) {
    try {
      loanService.createOffer(loanId, request);
      model.addAttribute("managerLoanSuccess", "Предложение создано.");
      model.addAttribute("loanOfferRequest", new LoanOfferRequest());
    } catch (RuntimeException e) {
      model.addAttribute("managerLoanError", e.getMessage());
      model.addAttribute("loanOfferRequest", request);
    }

    model.addAttribute("pendingLoans", getPendingLoanViews());
    return "manager/loans";
  }

  @PostMapping("/manager/loans/{loanId}/reject")
  public String rejectApplication(@PathVariable Long loanId, RedirectAttributes redirectAttributes) {
    if (loanService.rejectApplication(loanId)) {
      redirectAttributes.addFlashAttribute("managerLoanSuccess", "Заявка отклонена.");
    } else {
      redirectAttributes.addFlashAttribute("managerLoanError", "Не удалось отклонить заявку.");
    }

    return "redirect:/manager/loans";
  }

  private List<LoanView> getPendingLoanViews() {
    return loanService.getPendingLoans()
        .stream()
        .map(this::toLoanView)
        .toList();
  }

  private LoanView toLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));

    return new LoanView(
        loan.getLoanId(),
        loanType.getName(),
        formatMoney(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatRate(loan.getRate()),
        loan.getDuration() == null ? "-" : loan.getDuration() + " мес.",
        loan.getMonthlyPayment() == null ? "-" : formatMoney(loan.getMonthlyPayment()) + " ₸",
        "На рассмотрении",
        "-",
        false
    );
  }

  private String formatMoney(BigDecimal amount) {
    if (amount == null) {
      return "0.00";
    }

    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
    symbols.setGroupingSeparator(' ');

    DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
    return format.format(amount);
  }

  private String formatRate(BigDecimal rate) {
    if (rate == null) {
      return "0%";
    }

    return rate.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%";
  }
}
