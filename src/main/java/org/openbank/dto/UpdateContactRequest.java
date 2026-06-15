package org.openbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UpdateContactRequest {

  @Pattern(regexp = "^$|^\\+?\\d[\\d\\s()-]{8,20}$", message = "Phone number format is invalid")
  private String phone;

  @Email(message = "Email format is invalid")
  @Size(max = 255, message = "Email must be shorter than 255 characters")
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
