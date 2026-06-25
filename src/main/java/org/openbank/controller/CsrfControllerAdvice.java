package org.openbank.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.SecureRandom;
import java.util.Base64;

@ControllerAdvice
public class CsrfControllerAdvice {

  private final SecureRandom secureRandom = new SecureRandom();

  @ModelAttribute("csrfToken")
  public String csrfToken(HttpSession session) {
    Object existingToken = session.getAttribute(SessionKeys.CSRF_TOKEN);
    if (existingToken instanceof String token && !token.isBlank()) {
      return token;
    }
    byte[] tokenBytes = new byte[32];
    secureRandom.nextBytes(tokenBytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    session.setAttribute(SessionKeys.CSRF_TOKEN, token);

    return token;
  }
}
