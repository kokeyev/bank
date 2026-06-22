package org.openbank.dao;

import org.openbank.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Defines the user dao contract.
 */
public interface UserDao {

  /** Creates a user. */
  boolean createNewUser(User user);

  /** Finds a user by id. */
  Optional<User> getUserById(Long userId);

  /** Finds a user by phone number. */
  Optional<User> getUserByPhoneNumber(String phoneNumber);

  /** Finds a user by email address. */
  Optional<User> getUserByEmailAddress(String emailAddress);

  /** Returns users by role and status. */
  List<User> getUsersByRoleAndStatus(String role, String status);

  /** Checks if a phone number already exists. */
  boolean existsByPhoneNumber(String phoneNumber);

  /** Checks if an email address already exists. */
  boolean existsByEmailAddress(String emailAddress);

  /** Changes a user phone number. */
  boolean changePhoneNumberOfUserById(Long userId, String newPhoneNumber);

  /** Changes a user email address. */
  boolean changeEmailAddressOfUserById(Long userId, String newEmailAddress);

  /** Changes a user password hash. */
  boolean changePasswordHashOfUserById(Long userId, String newPasswordHash);

  /** Changes a user status. */
  boolean changeStatusOfUserById(Long userId, String status);

  /** Deletes a user by id. */
  boolean deleteUserById(Long userId);

}
