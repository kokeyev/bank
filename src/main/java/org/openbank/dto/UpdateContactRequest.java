package org.openbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateContactRequest {

  @Pattern(regexp = "^$|^\\+?\\d[\\d\\s()-]{8,20}$", message = "{validation.phone.format}")
  private String phone;

  @Email(message = "{validation.email.format}")
  @Size(max = 255, message = "{validation.email.size}")
  private String email;

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
