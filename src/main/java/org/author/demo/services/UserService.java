package org.author.demo.services;

import org.author.demo.dao.user.UserDao;
import org.author.demo.dto.CreateUserRequest;
import org.author.demo.dto.PasswordChangeRequest;
import org.author.demo.dto.UpdateContactRequest;
import org.author.demo.model.User;
import org.author.demo.model.status.UserStatus;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UserService {

  private static final String DEFAULT_ROLE = "CLIENT";
  private static final String MANAGER_ROLE = "MANAGER";
  private static final int PASSWORD_HASH_ITERATIONS = 120_000;
  private static final int PASSWORD_KEY_LENGTH = 256;
  private static final int SALT_BYTES = 16;

  private final UserDao userDao;
  private final SecureRandom secureRandom = new SecureRandom();

  public UserService(UserDao userDao) {
    this.userDao = userDao;
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
    user.setPassword_hash(hashPassword(request.getPassword()));

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
    user.setPassword_hash(hashPassword(request.getPassword()));

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

    if (user.isEmpty()
        || !UserStatus.ACTIVE.name().equals(user.get().getStatus())
        || !passwordMatches(password, user.get().getPassword_hash())) {
      return Optional.empty();
    }

    return user;
  }

  public Optional<User> authenticateManager(String login, String password) {
    Optional<User> user = authenticateStaff(login, password);

    if (user.isEmpty()
        || !MANAGER_ROLE.equals(user.get().getRole())
        || !UserStatus.ACTIVE.name().equals(user.get().getStatus())) {
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
    if (!passwordMatches(request.getCurrentPassword(), currentUser.getPassword_hash())) {
      errors.add("Текущий пароль указан неверно.");
    }

    if (!errors.isEmpty()) {
      throw new ContactUpdateException(errors);
    }

    userDao.changePasswordHashOfUserById(currentUser.getUserId(), hashPassword(request.getNewPassword()));
  }

  public void deactivateUser(Long userId) {
    if (!userDao.changeStatusOfUserById(userId, UserStatus.DEACTIVATED.name())) {
      throw new IllegalStateException("Не удалось деактивировать аккаунт");
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

    if (user.isEmpty() || !passwordMatches(password, user.get().getPassword_hash())) {
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

  private String hashPassword(String password) {
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);

    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PASSWORD_HASH_ITERATIONS, PASSWORD_KEY_LENGTH);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] hash = factory.generateSecret(spec).getEncoded();

      return "pbkdf2_sha256$"
          + PASSWORD_HASH_ITERATIONS
          + "$"
          + Base64.getEncoder().encodeToString(salt)
          + "$"
          + Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new IllegalStateException("Не удалось обработать пароль", e);
    }
  }

  private boolean passwordMatches(String password, String storedPasswordHash) {
    if (storedPasswordHash == null || !storedPasswordHash.startsWith("pbkdf2_sha256$")) {
      return false;
    }

    String[] parts = storedPasswordHash.split("\\$");
    if (parts.length != 4) {
      return false;
    }

    try {
      int iterations = Integer.parseInt(parts[1]);
      byte[] salt = Base64.getDecoder().decode(parts[2]);
      byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, expectedHash.length * 8);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] actualHash = factory.generateSecret(spec).getEncoded();

      return java.security.MessageDigest.isEqual(expectedHash, actualHash);
    } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      return false;
    }
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
