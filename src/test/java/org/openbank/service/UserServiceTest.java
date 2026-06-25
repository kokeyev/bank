package org.openbank.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.UserDao;
import org.openbank.dto.CreateUserRequest;
import org.openbank.dto.PasswordChangeRequest;
import org.openbank.dto.UpdateContactRequest;
import org.openbank.exception.ContactUpdateException;
import org.openbank.exception.UserRegistrationException;
import org.openbank.model.User;
import org.openbank.model.status.UserStatus;
import org.openbank.service.impl.UserServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final Long USER_ID = 1L;
  private static final Long CLIENT_ID = 2L;
  private static final Long PENDING_MANAGER_ID = 3L;
  private static final String NAME = "Aruzhan";
  private static final String SURNAME = "Sadyk";
  private static final String PHONE_NUMBER = "+77001112233";
  private static final String DUPLICATE_PHONE_NUMBER = "+77009998877";
  private static final String EMAIL = "aru@example.com";
  private static final String RAW_EMAIL = "ARU@example.com";
  private static final String EMAIL_WITH_SPACES = " ARU@example.com ";
  private static final String MANAGER_EMAIL = "manager@example.com";
  private static final String CLIENT_EMAIL = "client@example.com";
  private static final String PENDING_EMAIL = "pending@example.com";
  private static final String NEW_EMAIL = "new@example.com";
  private static final String RAW_NEW_EMAIL = "NEW@example.com";
  private static final String CLIENT_ROLE = "CLIENT";
  private static final String MANAGER_ROLE = "MANAGER";
  private static final String PASSWORD = "password123";
  private static final String PASSWORD_HASH = "hash";
  private static final String WRONG_PASSWORD = "wrong";
  private static final String OLD_PASSWORD = "old-password";
  private static final String NEW_PASSWORD = "new-password";
  private static final String NEW_PASSWORD_HASH = "new-hash";
  private static final String SHORT_PASSWORD = "short";
  private static final String DIFFERENT_PASSWORD = "different";
  private static final String BLANK_LOGIN = " ";
  private static final String BLANK_PASSWORD = " ";
  private static final String EMPTY_VALUE = "";

  @Mock
  private UserDao userDao;

  @Mock
  private PasswordHasher passwordHasher;

  @Mock
  private MessageService messageService;

  @InjectMocks
  private UserServiceImpl service;

  @BeforeEach
  void setUpMessages() {
    lenient().when(messageService.get(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(messageService.get(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void createUserNormalizesEmailAndStoresHashedPassword() {
    when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);

    service.createUser(validRequest());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userDao).createNewUser(captor.capture());
    assertEquals(EMAIL, captor.getValue().getEmailAddress());
    assertEquals(CLIENT_ROLE, captor.getValue().getRole());
    assertEquals(UserStatus.ACTIVE.name(), captor.getValue().getStatus());
    assertEquals(PASSWORD_HASH, captor.getValue().getPasswordHash());
  }

  @Test
  void createUserRejectsDuplicatePhoneAndEmail() {
    CreateUserRequest request = validRequest();
    when(userDao.existsByPhoneNumber(PHONE_NUMBER)).thenReturn(true);
    when(userDao.existsByEmailAddress(EMAIL)).thenReturn(true);

    Executable executable = () -> service.createUser(request);
    assertThrows(UserRegistrationException.class, executable);
    verify(userDao, never()).createNewUser(any());
  }

  @Test
  void createManagerStoresPendingManagerRole() {
    when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);

    service.createManager(validRequest());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userDao).createNewUser(captor.capture());
    assertEquals(MANAGER_ROLE, captor.getValue().getRole());
    assertEquals(UserStatus.PENDING.name(), captor.getValue().getStatus());
  }

  @Test
  void authenticateReturnsEmptyForBlankInputAndMissingUser() {
    assertTrue(service.authenticate(BLANK_LOGIN, PASSWORD).isEmpty());
    assertTrue(service.authenticate(EMAIL, BLANK_PASSWORD).isEmpty());
    when(userDao.getUserByEmailAddress(EMAIL)).thenReturn(Optional.empty());

    assertTrue(service.authenticate(EMAIL, PASSWORD).isEmpty());
  }

  @Test
  void authenticateAcceptsActiveUserWithMatchingPassword() {
    User user = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    when(userDao.getUserByEmailAddress(EMAIL)).thenReturn(Optional.of(user));
    when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

    Optional<User> authenticated = service.authenticate(EMAIL_WITH_SPACES, PASSWORD);

    assertTrue(authenticated.isPresent());
  }

  @Test
  void authenticateRejectsInactiveUser() {
    User user = user(USER_ID, CLIENT_ROLE, UserStatus.DEACTIVATED.name());
    when(userDao.getUserByPhoneNumber(PHONE_NUMBER)).thenReturn(Optional.of(user));

    assertTrue(service.authenticate(PHONE_NUMBER, PASSWORD).isEmpty());
  }

  @Test
  void authenticateRejectsWrongPassword() {
    User user = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    when(userDao.getUserByEmailAddress(EMAIL)).thenReturn(Optional.of(user));
    when(passwordHasher.matches(WRONG_PASSWORD, PASSWORD_HASH)).thenReturn(false);

    assertTrue(service.authenticate(EMAIL, WRONG_PASSWORD).isEmpty());
  }

  @Test
  void authenticateManagerRequiresManagerRoleAndActiveStatus() {
    User manager = user(USER_ID, MANAGER_ROLE, UserStatus.ACTIVE.name());
    when(userDao.getUserByEmailAddress(MANAGER_EMAIL)).thenReturn(Optional.of(manager));
    when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

    assertTrue(service.authenticateManager(MANAGER_EMAIL, PASSWORD).isPresent());

    User client = user(CLIENT_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    when(userDao.getUserByEmailAddress(CLIENT_EMAIL)).thenReturn(Optional.of(client));
    when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

    assertTrue(service.authenticateManager(CLIENT_EMAIL, PASSWORD).isEmpty());

    User inactiveManager = user(PENDING_MANAGER_ID, MANAGER_ROLE, UserStatus.PENDING.name());
    when(userDao.getUserByEmailAddress(PENDING_EMAIL)).thenReturn(Optional.of(inactiveManager));
    when(passwordHasher.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

    assertTrue(service.authenticateManager(PENDING_EMAIL, PASSWORD).isEmpty());
  }

  @Test
  void changePasswordValidatesCurrentPasswordAndUpdatesHash() {
    User currentUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    PasswordChangeRequest request = new PasswordChangeRequest();
    request.setCurrentPassword(OLD_PASSWORD);
    request.setNewPassword(NEW_PASSWORD);
    request.setConfirmPassword(NEW_PASSWORD);
    when(passwordHasher.matches(OLD_PASSWORD, PASSWORD_HASH)).thenReturn(true);
    when(passwordHasher.hash(NEW_PASSWORD)).thenReturn(NEW_PASSWORD_HASH);

    service.changePassword(currentUser, request);

    verify(userDao).changePasswordHashOfUserById(USER_ID, NEW_PASSWORD_HASH);
  }

  @Test
  void changePasswordCollectsValidationErrors() {
    User currentUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    PasswordChangeRequest request = new PasswordChangeRequest();
    request.setCurrentPassword(EMPTY_VALUE);
    request.setNewPassword(SHORT_PASSWORD);
    request.setConfirmPassword(DIFFERENT_PASSWORD);
    when(passwordHasher.matches(EMPTY_VALUE, PASSWORD_HASH)).thenReturn(false);

    assertThrows(ContactUpdateException.class, () -> service.changePassword(currentUser, request));

    verify(userDao, never()).changePasswordHashOfUserById(any(), any());
  }

  @Test
  void updateContactDetailsRejectsDuplicateEmail() {
    User currentUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    UpdateContactRequest request = new UpdateContactRequest();
    request.setPhone(currentUser.getPhoneNumber());
    request.setEmail(NEW_EMAIL);
    when(userDao.existsByEmailAddress(NEW_EMAIL)).thenReturn(true);

    Executable executable = () -> service.updateContactDetails(currentUser, request);
    assertThrows(ContactUpdateException.class, executable);
  }

  @Test
  void updateContactDetailsRejectsEmptyAndDuplicatePhone() {
    User currentUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    UpdateContactRequest emptyRequest = new UpdateContactRequest();
    emptyRequest.setPhone(EMPTY_VALUE);
    emptyRequest.setEmail(EMPTY_VALUE);

    Executable executable = () -> service.updateContactDetails(currentUser, emptyRequest);
    assertThrows(ContactUpdateException.class, executable);

    UpdateContactRequest duplicatePhone = new UpdateContactRequest();
    duplicatePhone.setPhone(DUPLICATE_PHONE_NUMBER);
    duplicatePhone.setEmail(currentUser.getEmailAddress());
    when(userDao.existsByPhoneNumber(DUPLICATE_PHONE_NUMBER)).thenReturn(true);

    Executable executable1 = () -> service.updateContactDetails(currentUser, duplicatePhone);
    assertThrows(ContactUpdateException.class, executable1);
  }

  @Test
  void updateContactDetailsChangesPhoneAndEmail() {
    User currentUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    User updatedUser = user(USER_ID, CLIENT_ROLE, UserStatus.ACTIVE.name());
    updatedUser.setPhoneNumber(DUPLICATE_PHONE_NUMBER);
    updatedUser.setEmailAddress(NEW_EMAIL);
    UpdateContactRequest request = new UpdateContactRequest();
    request.setPhone(DUPLICATE_PHONE_NUMBER);
    request.setEmail(RAW_NEW_EMAIL);
    when(userDao.getUserById(USER_ID)).thenReturn(Optional.of(updatedUser));

    User result = service.updateContactDetails(currentUser, request);

    assertEquals(DUPLICATE_PHONE_NUMBER, result.getPhoneNumber());
    assertEquals(NEW_EMAIL, result.getEmailAddress());
    verify(userDao).changePhoneNumberOfUserById(USER_ID, DUPLICATE_PHONE_NUMBER);
    verify(userDao).changeEmailAddressOfUserById(USER_ID, NEW_EMAIL);
  }

  private CreateUserRequest validRequest() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName(NAME);
    request.setSurname(SURNAME);
    request.setPhone(PHONE_NUMBER);
    request.setEmail(RAW_EMAIL);
    request.setPassword(PASSWORD);
    request.setConfirmPassword(PASSWORD);

    return request;
  }

  private User user(Long id, String role, String status) {
    return new User(id, NAME, SURNAME, PHONE_NUMBER, EMAIL, role, status, LocalDate.now(), null, PASSWORD_HASH);
  }
}
