package org.openbank.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbank.dao.user.UserDao;
import org.openbank.dto.CreateUserRequest;
import org.openbank.dto.PasswordChangeRequest;
import org.openbank.dto.UpdateContactRequest;
import org.openbank.exception.ContactUpdateException;
import org.openbank.exception.UserRegistrationException;
import org.openbank.model.User;
import org.openbank.model.status.UserStatus;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserDao userDao;

  @Mock
  private PasswordHasher passwordHasher;

  @InjectMocks
  private UserService service;

  @Test
  void createUserNormalizesEmailAndStoresHashedPassword() {
    when(passwordHasher.hash("password123")).thenReturn("hash");

    service.createUser(validRequest());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userDao).createNewUser(captor.capture());
    assertEquals("aru@example.com", captor.getValue().getEmailAddress());
    assertEquals("CLIENT", captor.getValue().getRole());
    assertEquals(UserStatus.ACTIVE.name(), captor.getValue().getStatus());
    assertEquals("hash", captor.getValue().getPasswordHash());
  }

  @Test
  void createUserRejectsDuplicatePhoneAndEmail() {
    CreateUserRequest request = validRequest();
    when(userDao.existsByPhoneNumber("+77001112233")).thenReturn(true);
    when(userDao.existsByEmailAddress("aru@example.com")).thenReturn(true);

    assertThrows(UserRegistrationException.class, () -> service.createUser(request));
    verify(userDao, never()).createNewUser(any());
  }

  @Test
  void authenticateAcceptsActiveUserWithMatchingPassword() {
    User user = user(1L, "CLIENT", UserStatus.ACTIVE.name());
    when(userDao.getUserByEmailAddress("aru@example.com")).thenReturn(Optional.of(user));
    when(passwordHasher.matches("password123", "hash")).thenReturn(true);

    Optional<User> authenticated = service.authenticate(" ARU@example.com ", "password123");

    assertTrue(authenticated.isPresent());
  }

  @Test
  void authenticateRejectsInactiveUser() {
    User user = user(1L, "CLIENT", UserStatus.DEACTIVATED.name());
    when(userDao.getUserByPhoneNumber("+77001112233")).thenReturn(Optional.of(user));

    assertTrue(service.authenticate("+77001112233", "password123").isEmpty());
  }

  @Test
  void changePasswordValidatesCurrentPasswordAndUpdatesHash() {
    User currentUser = user(1L, "CLIENT", UserStatus.ACTIVE.name());
    PasswordChangeRequest request = new PasswordChangeRequest();
    request.setCurrentPassword("old-password");
    request.setNewPassword("new-password");
    request.setConfirmPassword("new-password");
    when(passwordHasher.matches("old-password", "hash")).thenReturn(true);
    when(passwordHasher.hash("new-password")).thenReturn("new-hash");

    service.changePassword(currentUser, request);

    verify(userDao).changePasswordHashOfUserById(1L, "new-hash");
  }

  @Test
  void updateContactDetailsRejectsDuplicateEmail() {
    User currentUser = user(1L, "CLIENT", UserStatus.ACTIVE.name());
    UpdateContactRequest request = new UpdateContactRequest();
    request.setPhone(currentUser.getPhoneNumber());
    request.setEmail("new@example.com");
    when(userDao.existsByEmailAddress("new@example.com")).thenReturn(true);

    assertThrows(ContactUpdateException.class, () -> service.updateContactDetails(currentUser, request));
  }

  private CreateUserRequest validRequest() {
    CreateUserRequest request = new CreateUserRequest();
    request.setName("Aruzhan");
    request.setSurname("Sadyk");
    request.setPhone("+77001112233");
    request.setEmail("ARU@example.com");
    request.setPassword("password123");
    request.setConfirmPassword("password123");
    return request;
  }

  private User user(Long id, String role, String status) {
    return new User(id, "Aruzhan", "Sadyk", "+77001112233", "aru@example.com", role, status, LocalDate.now(), null, "hash");
  }
}
