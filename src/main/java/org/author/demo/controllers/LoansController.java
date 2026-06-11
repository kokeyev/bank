package org.author.demo.controllers;

import jakarta.servlet.http.HttpSession;
import org.author.demo.dto.LoanApplicationRequest;
import org.author.demo.dto.LoanPaymentScheduleItem;
import org.author.demo.dto.LoanTypeView;
import org.author.demo.dto.LoanView;
import org.author.demo.model.Loan;
import org.author.demo.model.LoanType;
import org.author.demo.model.User;
import org.author.demo.model.status.LoanStatus;
import org.author.demo.services.CurrentUserService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class LoansController {

  private static final String PURPOSE = "На любые цели";
  private static final String AUTO = "Автокредит";
  private static final String MORTGAGE = "Ипотека";
  private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final CurrentUserService currentUserService;
  private final LoanService loanService;

  public LoansController(CurrentUserService currentUserService, LoanService loanService) {
    this.currentUserService = currentUserService;
    this.loanService = loanService;
  }

  @GetMapping("/loans")
  public String loans(HttpSession session, Model model) {
    model.addAttribute("loanTypes", getLoanTypeViews());

    Optional<User> currentUser = currentUserService.getCurrentUser(session);
    currentUser.ifPresent(user -> model.addAttribute("loans", getLoanViews(user.getUserId())));

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
  public String acceptLoanOffer(@PathVariable Long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
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
  public String rejectLoanOffer(@PathVariable Long loanId, HttpSession session, RedirectAttributes redirectAttributes) {
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
    } catch (RuntimeException e) {
      model.addAttribute("loanError", e.getMessage());
      addLoanFormModel(model, loanTypeName, request);
    }

    return template;
  }

  private void addLoanFormModel(Model model, String loanTypeName, LoanApplicationRequest request) {
    model.addAttribute("loanApplicationRequest", request);
    loanService.getLoanTypeByName(loanTypeName)
        .ifPresent(loanType -> model.addAttribute("loanType", toLoanTypeView(loanType)));
  }

  private List<LoanTypeView> getLoanTypeViews() {
    return loanService.getAllLoanTypes()
        .stream()
        .map(this::toLoanTypeView)
        .toList();
  }

  private List<LoanView> getLoanViews(Long userId) {
    return loanService.getLoansByUserId(userId)
        .stream()
        .map(this::toLoanView)
        .toList();
  }

  private LoanTypeView toLoanTypeView(LoanType loanType) {
    return new LoanTypeView(
        loanType.getLoanTypeId(),
        loanType.getName(),
        getLoanSlug(loanType.getName()),
        getLoanTag(loanType.getName()),
        getLoanDescription(loanType.getName()),
        "от " + formatMoney(loanType.getMinimumAmount()) + " до " + formatMoney(loanType.getMaximumAmount()) + " ₸",
        "до " + formatDuration(loanType.getDuration()),
        "от " + formatRate(loanType.getRate())
    );
  }

  private LoanView toLoanView(Loan loan) {
    LoanType loanType = loanService.getLoanTypeById(loan.getLoanTypeId())
        .orElseThrow(() -> new IllegalStateException("Тип кредита не найден"));

    return new LoanView(
        loan.getLoanId(),
        loanType.getName(),
        formatMoney(loan.getRemainingAmount()) + " ₸",
        loan.getRate() == null ? "-" : formatRate(loan.getRate()),
        loan.getDuration() == null ? "-" : formatDuration(loan.getDuration()),
        loan.getMonthlyPayment() == null ? "-" : formatMoney(loan.getMonthlyPayment()) + " ₸",
        translateStatus(loan.getStatus()),
        formatDate(loan.getStartDate()),
        LoanStatus.OFFERED.name().equals(loan.getStatus()),
        LoanStatus.ACTIVE.name().equals(loan.getStatus()),
        formatMoney(loanService.calculateLatePenalty(loan)) + " ₸",
        getScheduleItems(loan)
    );
  }

  private List<LoanPaymentScheduleItem> getScheduleItems(Loan loan) {
    if (!LoanStatus.ACTIVE.name().equals(loan.getStatus()) || loan.getMonthlyPayment() == null) {
      return List.of();
    }

    List<LocalDate> dueDates = loanService.getPaymentDueDates(loan);
    return java.util.stream.IntStream.range(0, dueDates.size())
        .mapToObj(index -> new LoanPaymentScheduleItem(
            index + 1,
            formatDate(dueDates.get(index)),
            formatMoney(loan.getMonthlyPayment()) + " ₸",
            dueDates.get(index).isBefore(LocalDate.now()) ? "Проверьте оплату" : "Ожидается"
        ))
        .toList();
  }

  private String getLoanSlug(String loanTypeName) {
    if (PURPOSE.equals(loanTypeName)) {
      return "purpose";
    }
    if (AUTO.equals(loanTypeName)) {
      return "auto";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "mortgage";
    }
    return "purpose";
  }

  private String getLoanTag(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "транспорт";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "жилье";
    }
    return "быстро";
  }

  private String getLoanDescription(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "Для покупки нового или поддержанного автомобиля с удобным графиком платежей.";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "Для покупки квартиры или дома с первоначальным взносом.";
    }
    return "Для покупок, ремонта, учебы или других личных планов.";
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

  private String formatDuration(Integer duration) {
    if (duration == null) {
      return "";
    }

    if (duration >= 12 && duration % 12 == 0) {
      int years = duration / 12;
      return years + " " + (years == 1 ? "год" : "лет");
    }

    return duration + " мес.";
  }

  private String formatDate(LocalDate date) {
    return date == null ? "-" : date.format(DISPLAY_DATE_FORMATTER);
  }

  private String translateStatus(String status) {
    if (LoanStatus.PENDING.name().equals(status)) {
      return "На рассмотрении";
    }
    if (LoanStatus.OFFERED.name().equals(status)) {
      return "Предложение";
    }
    if (LoanStatus.ACTIVE.name().equals(status)) {
      return "Активен";
    }
    if (LoanStatus.REFUSED.name().equals(status)) {
      return "Отклонен клиентом";
    }
    if (LoanStatus.REJECTED.name().equals(status)) {
      return "Отклонен менеджером";
    }
    if (LoanStatus.CLOSED.name().equals(status)) {
      return "Закрыт";
    }
    return status == null ? "" : status;
  }
}
