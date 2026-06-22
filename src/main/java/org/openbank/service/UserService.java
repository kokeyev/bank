package org.openbank.service;

import org.openbank.dto.CreateUserRequest;
import org.openbank.dto.PasswordChangeRequest;
import org.openbank.dto.UpdateContactRequest;
import org.openbank.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Defines user registration, authentication, and profile operations.
 */
public interface UserService {

  /** Registers an active client account. */
  void createUser(CreateUserRequest request);

  /** Registers a manager account awaiting approval. */
  void createManager(CreateUserRequest request);

  /** Loads a user by database id. */
  Optional<User> getUserById(Long userId);

  /** Authenticates an active client. */
  Optional<User> authenticate(String login, String password);

  /** Authenticates an active manager. */
  Optional<User> authenticateManager(String login, String password);

  /** Authenticates an active admin. */
  Optional<User> authenticateAdmin(String login, String password);

  /** Returns manager registrations waiting for approval. */
  List<User> getPendingManagers();

  /** Activates a pending manager account. */
  boolean approveManager(Long managerId);

  /** Rejects a manager registration. */
  boolean rejectManager(Long managerId);

  /** Changes a user's password after validation. */
  void changePassword(User currentUser, PasswordChangeRequest request);

  /** Deactivates a user account. */
  void deactivateUser(Long userId);

  /** Updates phone and email contact details. */
  User updateContactDetails(User currentUser, UpdateContactRequest request);
}
