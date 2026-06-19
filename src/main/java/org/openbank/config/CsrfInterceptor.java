package org.openbank.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.openbank.controller.SessionKeys;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CsrfInterceptor implements HandlerInterceptor {

  private static final String CSRF_PARAMETER = "_csrf";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    HttpSession session = request.getSession(false);
    String expectedToken = session == null ? null : (String) session.getAttribute(SessionKeys.CSRF_TOKEN);
    String actualToken = request.getParameter(CSRF_PARAMETER);

    if (expectedToken == null || !expectedToken.equals(actualToken)) {
      throw new IllegalArgumentException("Invalid form token. Refresh the page and try again.");
    }

    return true;
  }
}
