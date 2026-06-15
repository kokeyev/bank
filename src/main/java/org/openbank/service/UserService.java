package org.openbank.service;

import org.openbank.dao.user.UserDao;
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

/**
 * Provides user service operations.
 */
@Service
public class UserService {

  private static final String DEFAULT_ROLE = "CLIENT";
  private static final String MANAGER_ROLE = "MANAGER";

  private final UserDao userDao;
  private final PasswordHasher passwordHasher;

  /**
   * Handles user service.
   */
  public UserService(UserDao userDao, PasswordHasher passwordHasher) {
    this.userDao = userDao;
    this.passwordHasher = passwordHasher;
  }

  /**
   * Handles create user.
   */
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

  /**
   * Handles create manager.
   */
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

  /**
   * Handles get user by id.
   */
  public Optional<User> getUserById(Long userId) {
    return userDao.getUserById(userId);
  }

  /**
   * Handles authenticate.
   */
  public Optional<User> authenticate(String login, String password) {
    if (isBlank(login) || isBlank(password)) {
      return Optional.empty();
    }

    String cleanedLogin = clean(login);
    Optional<User> user = cleanedLogin.contains("@")
        ? userDao.getUserByEmailAddress(cleanedLogin.toLowerCase(Locale.ROOT))
        : userDao.getUserByPhoneNumber(cleanedLogin);

    if (user.isEmpty()
        || !UserStatus.ACTIVE.name().equals(user.get().getStatus())
        || !passwordHasher.matches(password, user.get().getPasswordHash())) {
      return Optional.empty();
    }

    return user;
  }

  /**
   * Handles authenticate manager.
   */
  public Optional<User> authenticateManager(String login, String password) {
    Optional<User> user = authenticateStaff(login, password);

    if (user.isEmpty()
        || !MANAGER_ROLE.equals(user.get().getRole())
        || !UserStatus.ACTIVE.name().equals(user.get().getStatus())) {
      return Optional.empty();
    }

    return user;
  }

  /**
   * Handles get pending managers.
   */
  public List<User> getPendingManagers() {
    return userDao.getUsersByRoleAndStatus(MANAGER_ROLE, UserStatus.PENDING.name());
  }

  /**
   * Handles approve manager.
   */
  public boolean approveManager(Long managerId) {
    return userDao.changeStatusOfUserById(managerId, UserStatus.ACTIVE.name());
  }

  /**
   * Handles reject manager.
   */
  public boolean rejectManager(Long managerId) {
    return userDao.changeStatusOfUserById(managerId, UserStatus.DEACTIVATED.name());
  }

  /**
   * Handles change password.
   */
  public void changePassword(User currentUser, PasswordChangeRequest request) {
    List<String> errors = new ArrayList<>();

    if (isBlank(request.getCurrentPassword())) {
      errors.add("Введите текущий пароль.");
    }
    if (isBlank(request.getNewPassword())) {
      errors.add("Введите новый пароль.");
    }
    if (!isBlank(request.getNewPassword()) && request.getNewPassword().length() < 8) {
      errors.add("Новый пароль должен содержать минимум 8 символов.");
    }
    if (!safeEquals(request.getNewPassword(), request.getConfirmPassword())) {
      errors.add("Новые пароли не совпадают.");
    }
    if (!passwordHasher.matches(request.getCurrentPassword(), currentUser.getPasswordHash())) {
      errors.add("Текущий пароль указан неверно.");
    }

    if (!errors.isEmpty()) {
      throw new ContactUpdateException(errors);
    }

    userDao.changePasswordHashOfUserById(currentUser.getUserId(), passwordHasher.hash(request.getNewPassword()));
  }

  /**
   * Handles deactivate user.
   */
  public void deactivateUser(Long userId) {
    if (!userDao.changeStatusOfUserById(userId, UserStatus.DEACTIVATED.name())) {
      throw new IllegalStateException("Не удалось деактивировать аккаунт");
    }
  }

  /**
   * Handles update contact details.
   */
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
      throw new ContactUpdateException(List.of("Введите новый номер телефона или новую почту."));
    }

    return userDao.getUserById(currentUser.getUserId())
        .orElseThrow(() -> new IllegalStateException("Пользователь не найден"));
  }

  private List<String> validate(CreateUserRequest request) {
    List<String> errors = new ArrayList<>();

    if (isBlank(request.getName())) {
      errors.add("Введите имя.");
    }
    if (isBlank(request.getSurname())) {
      errors.add("Введите фамилию.");
    }
    if (isBlank(request.getPhone())) {
      errors.add("Введите номер телефона.");
    }
    if (isBlank(request.getEmail())) {
      errors.add("Введите почту.");
    }
    if (isBlank(request.getPassword())) {
      errors.add("Введите пароль.");
    }
    if (!isBlank(request.getPassword()) && request.getPassword().length() < 8) {
      errors.add("Пароль должен содержать минимум 8 символов.");
    }
    if (!safeEquals(request.getPassword(), request.getConfirmPassword())) {
      errors.add("Пароли не совпадают.");
    }

    String phone = clean(request.getPhone());
    if (!phone.isEmpty() && userDao.existsByPhoneNumber(phone)) {
      errors.add("Пользователь с таким номером телефона уже существует.");
    }

    String email = clean(request.getEmail()).toLowerCase(Locale.ROOT);
    if (!email.isEmpty() && userDao.existsByEmailAddress(email)) {
      errors.add("Пользователь с такой почтой уже существует.");
    }

    return errors;
  }

  private Optional<User> authenticateStaff(String login, String password) {
    if (isBlank(login) || isBlank(password)) {
      return Optional.empty();
    }

    String cleanedLogin = clean(login);
    Optional<User> user = cleanedLogin.contains("@")
        ? userDao.getUserByEmailAddress(cleanedLogin.toLowerCase(Locale.ROOT))
        : userDao.getUserByPhoneNumber(cleanedLogin);

    if (user.isEmpty() || !passwordHasher.matches(password, user.get().getPasswordHash())) {
      return Optional.empty();
    }

    return user;
  }

  private List<String> validateContactUpdate(User currentUser, String newPhone, String newEmail) {
    List<String> errors = new ArrayList<>();

    if (newPhone.isEmpty() && newEmail.isEmpty()) {
      errors.add("Введите новый номер телефона или новую почту.");
      return errors;
    }

    if (!newPhone.isEmpty()
        && !newPhone.equals(currentUser.getPhoneNumber())
        && userDao.existsByPhoneNumber(newPhone)) {
      errors.add("Пользователь с таким номером телефона уже существует.");
    }

    if (!newEmail.isEmpty()
        && !newEmail.equals(currentUser.getEmailAddress())
        && userDao.existsByEmailAddress(newEmail)) {
      errors.add("Пользователь с такой почтой уже существует.");
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
