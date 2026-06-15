package org.openbank.view;

import org.springframework.stereotype.Component;

@Component
public class LoanProductText {

  private static final String AUTO = "Автокредит";
  private static final String MORTGAGE = "Ипотека";

  public String slug(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "auto";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "mortgage";
    }
    return "purpose";
  }

  public String tag(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "транспорт";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "жилье";
    }
    return "быстро";
  }

  public String description(String loanTypeName) {
    if (AUTO.equals(loanTypeName)) {
      return "Для покупки нового или поддержанного автомобиля с удобным графиком платежей.";
    }
    if (MORTGAGE.equals(loanTypeName)) {
      return "Для покупки квартиры или дома с первоначальным взносом.";
    }
    return "Для покупок, ремонта, учебы или других личных планов.";
  }
}
