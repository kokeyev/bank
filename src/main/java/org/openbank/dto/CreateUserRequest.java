package org.openbank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

  @NotBlank(message = "{validation.name.required}")
  @Size(max = 100, message = "{validation.name.size}")
  private String name;

  @NotBlank(message = "{validation.surname.required}")
  @Size(max = 100, message = "{validation.surname.size}")
  private String surname;

  @NotBlank(message = "{validation.phone.required}")
  @Pattern(regexp = "^\\+?\\d[\\d\\s()-]{8,20}$", message = "{validation.phone.format}")
  private String phone;

  @NotBlank(message = "{validation.email.required}")
  @Email(message = "{validation.email.format}")
  @Size(max = 255, message = "{validation.email.size}")
  private String email;

  @NotBlank(message = "{validation.password.required}")
  @Size(min = 8, max = 72, message = "{validation.password.size}")
  private String password;

  @NotBlank(message = "{validation.password.confirm.required}")
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
