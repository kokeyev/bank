package org.openbank.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.openbank.service.CurrentUserService;
import org.openbank.service.StaffSessionService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Set;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

  private static final Set<String> PUBLIC_POST_PATHS = Set.of(
      "/login",
      "/register",
      "/admin/login",
      "/manager/login",
      "/manager/register"
  );

  private final CurrentUserService currentUserService;
  private final StaffSessionService staffSessionService;

  public LoginRequiredInterceptor(CurrentUserService currentUserService, StaffSessionService staffSessionService) {
    this.currentUserService = currentUserService;
    this.staffSessionService = staffSessionService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
    String path = request.getServletPath();

    if (isAdminArea(path)) {
      if (!staffSessionService.isAdminLoggedIn(request.getSession())) {
        response.sendRedirect(request.getContextPath() + "/admin");
        return false;
      }
      return true;
    }

    if (isManagerArea(path)) {
      if (!staffSessionService.isManagerLoggedIn(request.getSession())) {
        response.sendRedirect(request.getContextPath() + "/manager");
        return false;
      }
      return true;
    }

    if (!"POST".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    if (PUBLIC_POST_PATHS.contains(path) || currentUserService.isLoggedIn(request.getSession())) {
      return true;
    }

    response.sendRedirect(request.getContextPath() + "/login?loginRequired=true");
    return false;
  }

  private boolean isAdminArea(String path) {
    return path.startsWith("/admin/") && !"/admin/login".equals(path);
  }

  private boolean isManagerArea(String path) {
    return path.startsWith("/manager/") && !"/manager/login".equals(path) && !"/manager/register".equals(path);
  }
}
