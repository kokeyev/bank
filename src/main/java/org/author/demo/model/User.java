package org.author.demo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public class User {

  private Long userId;
  private String name;
  private String surname;
  private String phoneNumber;
  private String emailAddress;
  private String role;
  private String status;
  private LocalDate dateCreated;
  private LocalDate dateModified;
  private String password_hash;

  public User(Long userId, String name, String surname, String phoneNumber, String emailAddress, String role, String status, LocalDate dateCreated, LocalDate dateModified, String password_hash) {
    this.userId = userId;
    this.name = name;
    this.surname = surname;
    this.phoneNumber = phoneNumber;
    this.emailAddress = emailAddress;
    this.role = role;
    this.status = status;
    this.dateCreated = dateCreated;
    this.dateModified = dateModified;
    this.password_hash = password_hash;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

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

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getDateCreated() {
    return dateCreated;
  }

  public void setDateCreated(LocalDate dateCreated) {
    this.dateCreated = dateCreated;
  }

  public LocalDate getDateModified() {
    return dateModified;
  }

  public void setDateModified(LocalDate dateModified) {
    this.dateModified = dateModified;
  }

  public String getPassword_hash() {
    return password_hash;
  }

  public void setPassword_hash(String password_hash) {
    this.password_hash = password_hash;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof User user)) return false;
    return Objects.equals(getUserId(), user.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getUserId());
  }

  @Override
  public String toString() {
    return "User{" +
        "userId=" + userId +
        ", name='" + name + '\'' +
        ", surname='" + surname + '\'' +
        ", phoneNumber='" + phoneNumber + '\'' +
        ", emailAddress='" + emailAddress + '\'' +
        ", role='" + role + '\'' +
        ", status='" + status + '\'' +
        ", dateCreated=" + dateCreated +
        ", dateModified=" + dateModified +
        ", password_hash='" + password_hash + '\'' +
        '}';
  }
}
