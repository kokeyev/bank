package org.openbank.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.openbank.exception.BankDataAccessException;
import org.openbank.exception.BankInfrastructureException;
import org.openbank.exception.BankTransactionException;
import org.openbank.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MessageService messageService;

  public GlobalExceptionHandler(MessageService messageService) {
    this.messageService = messageService;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public String handleUserError(IllegalArgumentException exception, HttpServletRequest request, Model model) {
    LOGGER.info("User input error at {}: {}", request.getRequestURI(), exception.getMessage());
    model.addAttribute("errorTitle", messageService.get("error.validationTitle"));
    model.addAttribute("errorMessage", exception.getMessage());

    return "error";
  }

  @ExceptionHandler({BankDataAccessException.class, BankInfrastructureException.class, BankTransactionException.class, IllegalStateException.class})
  public ModelAndView handleTechnicalError(RuntimeException exception, HttpServletRequest request) {
    LOGGER.error("Technical error at {}", request.getRequestURI(), exception);

    return errorView(messageService.get("error.serviceTitle"), messageService.get("error.technicalMessage"));
  }

  @ExceptionHandler(Exception.class)
  public ModelAndView handleUnexpectedError(Exception exception, HttpServletRequest request) {
    LOGGER.error("Unexpected error at {}", request.getRequestURI(), exception);

    return errorView(messageService.get("error.unexpectedTitle"), messageService.get("error.technicalMessage"));
  }

  private ModelAndView errorView(String title, String message) {
    ModelAndView modelAndView = new ModelAndView("error");
    modelAndView.addObject("errorTitle", title);
    modelAndView.addObject("errorMessage", message);

    return modelAndView;
  }
}
