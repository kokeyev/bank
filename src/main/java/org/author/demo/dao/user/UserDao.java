package org.author.demo.dao.user;

import org.author.demo.model.User;

import java.util.List;
import java.util.Optional;



public interface UserDao {

  // boolean ?
  boolean createNewUser(User user);

  Optional<User> getUserById(Long userId);

  Optional<User> getUserByPhoneNumber(String phoneNumber);

  Optional<User> getUserByEmailAddress(String emailAddress);

  List<User> getUsersByRoleAndStatus(String role, String status);

  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByEmailAddress(String emailAddress);

  boolean changePhoneNumberOfUserById(Long user_id, String newPhoneNumber);

  boolean changeEmailAddressOfUserById(Long user_id, String newEmailAddress);

  boolean changePasswordHashOfUserById(Long user_id, String newPasswordHash);

  boolean changeStatusOfUserById(Long user_id, String status);

  boolean deleteUserById(Long user_id);

  // add exists methods ?


}
