package org.openbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PasswordChangeRequest {

  @NotBlank(message = "{validation.currentPassword.required}")
  private String currentPassword;

  @NotBlank(message = "{validation.newPassword.required}")
  @Size(min = 8, max = 72, message = "{validation.newPassword.size}")
  private String newPassword;

  @NotBlank(message = "{validation.password.confirm.required}")
  private String confirmPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }
}
