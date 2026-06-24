package org.openbank.service.impl;

import org.openbank.service.PasswordHasher;
import org.openbank.service.UserService;
import org.openbank.service.MessageService;
import org.openbank.dao.UserDao;
import org.openbank.dto.CreateUserRequest;
import org.openbank.dto.PasswordChangeRequest;
import org.openbank.dto.UpdateContactRequest;
import org.openbank.exception.ContactUpdateException;
import org.openbank.exception.UserRegistrationException;
import org.openbank.model.User;
import org.openbank.model.status.UserStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

  private static final String DEFAULT_ROLE = "CLIENT";
  private static final String MANAGER_ROLE = "MANAGER";
  private static final String ADMIN_ROLE = "ADMIN";

  private final UserDao userDao;
  private final PasswordHasher passwordHasher;
  private final MessageService messageService;

  public UserServiceImpl(UserDao userDao, PasswordHasher passwordHasher, MessageService messageService) {
    this.userDao = userDao;
    this.passwordHasher = passwordHasher;
    this.messageService = messageService;
  }

  public void createUser(CreateUserRequest request) {
    List<String> errors = validate(request);

    if (!errors.isEmpty()) {
      throw new UserRegistrationException(errors);
    }

    User user = new User();
    user.setName(clean(request.getName()));
    user.setSurname(clean(request.getSurname()));
    user.setPhoneNumber(clean(request.getPhone()));
    user.setEmailAddress(clean(request.getEmail()).toLowerCase(Locale.ROOT));
    user.setRole(DEFAULT_ROLE);
    user.setStatus(UserStatus.ACTIVE.name());
    user.setDateCreated(LocalDate.now());
    user.setPasswordHash(passwordHasher.hash(request.getPassword()));

    userDao.createNewUser(user);
  }

  public void createManager(CreateUserRequest request) {
    List<String> errors = validate(request);

    if (!errors.isEmpty()) {
      throw new UserRegistrationException(errors);
    }

    User user = new User();
    user.setName(clean(request.getName()));
    user.setSurname(clean(request.getSurname()));
    user.setPhoneNumber(clean(request.getPhone()));
    user.setEmailAddress(clean(request.getEmail()).toLowerCase(Locale.ROOT));
    user.setRole(MANAGER_ROLE);
    user.setStatus(UserStatus.PENDING.name());
    user.setDateCreated(LocalDate.now());
    user.setPasswordHash(passwordHasher.hash(request.getPassword()));

    userDao.createNewUser(user);
  }

  public Optional<User> getUserById(Long userId) {
    return userDao.getUserById(userId);
  }

  public Optional<User> authenticate(String login, String password) {
    if (isBlank(login) || isBlank(password)) {
      return Optional.empty();
    }

    String cleanedLogin = clean(login);
    Optional<User> user = cleanedLogin.contains("@")
        ? userDao.getUserByEmailAddress(cleanedLogin.toLowerCase(Locale.ROOT))
        : userDao.getUserByPhoneNumber(cleanedLogin);

    if (user.isEmpty() || !UserStatus.ACTIVE.name().equals(user.get().getStatus()) || !passwordHasher.matches(password, user.get().getPasswordHash())) {
      return Optional.empty();
    }

    return user;
  }

  public Optional<User> authenticateManager(String login, String password) {
    Optional<User> user = authenticateStaff(login, password);

    if (user.isEmpty() || !MANAGER_ROLE.equals(user.get().getRole()) || !UserStatus.ACTIVE.name().equals(user.get().getStatus())) {
      return Optional.empty();
    }

    return user;
  }

  public Optional<User> authenticateAdmin(String login, String password) {
    Optional<User> user = authenticateStaff(login, password);

    if (user.isEmpty() || !ADMIN_ROLE.equals(user.get().getRole()) || !UserStatus.ACTIVE.name().equals(user.get().getStatus())) {
      return Optional.empty();
    }

    return user;
  }

  public List<User> getPendingManagers() {
    return userDao.getUsersByRoleAndStatus(MANAGER_ROLE, UserStatus.PENDING.name());
  }

  public boolean approveManager(Long managerId) {
    return userDao.changeStatusOfUserById(managerId, UserStatus.ACTIVE.name());
  }

  public boolean rejectManager(Long managerId) {
    return userDao.changeStatusOfUserById(managerId, UserStatus.DEACTIVATED.name());
  }

  public void changePassword(User currentUser, PasswordChangeRequest request) {
    List<String> errors = new ArrayList<>();

    if (isBlank(request.getCurrentPassword())) {
      errors.add(messageService.get("validation.currentPassword.required"));
    }
    if (isBlank(request.getNewPassword())) {
      errors.add(messageService.get("validation.newPassword.required"));
    }
    if (!isBlank(request.getNewPassword()) && request.getNewPassword().length() < 8) {
      errors.add(messageService.get("validation.newPassword.min"));
    }
    if (!safeEquals(request.getNewPassword(), request.getConfirmPassword())) {
      errors.add(messageService.get("validation.newPassword.confirm.mismatch"));
    }
    if (!passwordHasher.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
      errors.add(messageService.get("validation.currentPassword.invalid"));
    }

    if (!errors.isEmpty()) {
      throw new ContactUpdateException(errors);
    }

    userDao.changePasswordHashOfUserById(currentUser.getUserId(), passwordHasher.hash(request.getNewPassword()));
  }

  public void deactivateUser(Long userId) {
    if (!userDao.changeStatusOfUserById(userId, UserStatus.DEACTIVATED.name())) {
      throw new IllegalStateException(messageService.get("settings.account.deactivate.error"));
    }
  }

  public User updateContactDetails(User currentUser, UpdateContactRequest request) {
    String newPhone = clean(request.getPhone());
    String newEmail = clean(request.getEmail()).toLowerCase(Locale.ROOT);
    List<String> errors = validateContactUpdate(currentUser, newPhone, newEmail);

    if (!errors.isEmpty()) {
      throw new ContactUpdateException(errors);
    }

    boolean changed = false;

    if (!newPhone.isEmpty() && !newPhone.equals(currentUser.getPhoneNumber())) {
      userDao.changePhoneNumberOfUserById(currentUser.getUserId(), newPhone);
      changed = true;
    }

    if (!newEmail.isEmpty() && !newEmail.equals(currentUser.getEmailAddress())) {
      userDao.changeEmailAddressOfUserById(currentUser.getUserId(), newEmail);
      changed = true;
    }

    if (!changed) {
      throw new ContactUpdateException(List.of(messageService.get("validation.contact.required")));
    }

    return userDao.getUserById(currentUser.getUserId())
        .orElseThrow(() -> new IllegalStateException(messageService.get("error.user.notFound")));
  }

  private List<String> validate(CreateUserRequest request) {
    List<String> errors = new ArrayList<>();

    if (isBlank(request.getName())) {
      errors.add(messageService.get("validation.name.required"));
    }
    if (isBlank(request.getSurname())) {
      errors.add(messageService.get("validation.surname.required"));
    }
    if (isBlank(request.getPhone())) {
      errors.add(messageService.get("validation.phone.required"));
    }
    if (isBlank(request.getEmail())) {
      errors.add(messageService.get("validation.email.required"));
    }
    if (isBlank(request.getPassword())) {
      errors.add(messageService.get("validation.password.required"));
    }
    if (!isBlank(request.getPassword()) && request.getPassword().length() < 8) {
      errors.add(messageService.get("validation.password.min"));
    }
    if (!safeEquals(request.getPassword(), request.getConfirmPassword())) {
      errors.add(messageService.get("validation.password.confirm.mismatch"));
    }

    String phone = clean(request.getPhone());
    if (!phone.isEmpty() && userDao.existsByPhoneNumber(phone)) {
      errors.add(messageService.get("validation.phone.duplicate"));
    }

    String email = clean(request.getEmail()).toLowerCase(Locale.ROOT);
    if (!email.isEmpty() && userDao.existsByEmailAddress(email)) {
      errors.add(messageService.get("validation.email.duplicate"));
    }

    return errors;
  }

  private Optional<User> authenticateStaff(String login, String password) {
    if (isBlank(login) || isBlank(password)) {
      return Optional.empty();
    }

    String cleanedLogin = clean(login);
    Optional<User> user;
    if (cleanedLogin.contains("@")) {
      user = userDao.getUserByEmailAddress(cleanedLogin.toLowerCase(Locale.ROOT));
    } else {
      user = userDao.getUserByPhoneNumber(cleanedLogin);
    }

    if (user.isEmpty() || !passwordHasher.matches(password, user.get().getPasswordHash())) {
      return Optional.empty();
    }

    return user;
  }

  private List<String> validateContactUpdate(User currentUser, String newPhone, String newEmail) {
    List<String> errors = new ArrayList<>();

    if (newPhone.isEmpty() && newEmail.isEmpty()) {
      errors.add(messageService.get("validation.contact.required"));
      return errors;
    }

    if (!newPhone.isEmpty() && !newPhone.equals(currentUser.getPhoneNumber()) && userDao.existsByPhoneNumber(newPhone)) {
      errors.add(messageService.get("validation.phone.duplicate"));
    }

    if (!newEmail.isEmpty() && !newEmail.equals(currentUser.getEmailAddress()) && userDao.existsByEmailAddress(newEmail)) {
      errors.add(messageService.get("validation.email.duplicate"));
    }

    return errors;
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  private String clean(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean safeEquals(String first, String second) {
    return first != null && first.equals(second);
  }
}
