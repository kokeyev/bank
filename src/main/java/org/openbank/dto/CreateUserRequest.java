package org.openbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

  @NotBlank(message = "Name is required")
  @Size(max = 100, message = "Name must be shorter than 100 characters")
  private String name;

  @NotBlank(message = "Surname is required")
  @Size(max = 100, message = "Surname must be shorter than 100 characters")
  private String surname;

  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\+?\\d[\\d\\s()-]{8,20}$", message = "Phone number format is invalid")
  private String phone;

  @NotBlank(message = "Email is required")
  @Email(message = "Email format is invalid")
  @Size(max = 255, message = "Email must be shorter than 255 characters")
  private String email;

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 72, message = "Password must contain from 8 to 72 characters")
  private String password;

  @NotBlank(message = "Password confirmation is required")
  private String confirmPassword;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

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

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getConfirmPassword() {
    return confirmPassword;
  }

  public void setConfirmPassword(String confirmPassword) {
    this.confirmPassword = confirmPassword;
  }
}
