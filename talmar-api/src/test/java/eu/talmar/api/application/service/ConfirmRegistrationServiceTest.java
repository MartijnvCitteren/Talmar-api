package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.IdentityFactory.PASSWORD_HASH;
import static eu.talmar.api.testSupport.IdentityFactory.RAW_TOKEN;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_HASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.ConfirmRegistrationCommand;
import eu.talmar.api.application.port.out.PasswordHasher;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.InvalidVerificationTokenException;
import eu.talmar.api.domain.identity.valueobject.Password;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.testSupport.IdentityFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmRegistrationServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-09T13:00:00Z"), ZoneOffset.UTC);
  private static final Clock CLOCK_AFTER_EXPIRY =
      Clock.fixed(Instant.parse("2026-10-11T12:00:00Z"), ZoneOffset.UTC);
  private static final Password PASSWORD = new Password("correct horse battery staple");

  @Mock private UserRepository userRepository;
  @Mock private PasswordHasher passwordHasher;

  private ConfirmRegistrationService service;

  @BeforeEach
  void setUp() {
    service = new ConfirmRegistrationService(userRepository, passwordHasher, CLOCK);
  }

  @Test
  @DisplayName(
      "given a pending owner and a valid token, when confirm then active with the hashed password"
          + " saved")
  void givenPendingOwnerAndValidTokenWhenConfirmThenActiveWithHashedPasswordSaved() {
    var user = IdentityFactory.pendingOwner(UUID.randomUUID());
    when(userRepository.findByVerificationTokenHash(TOKEN_HASH)).thenReturn(Optional.of(user));
    when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);

    var status = service.confirm(new ConfirmRegistrationCommand(RAW_TOKEN, PASSWORD));

    assertEquals(UserStatus.ACTIVE, status);
    assertEquals(new UserCredentials(PASSWORD_HASH), user.getCredentials());
    assertNull(user.getVerificationToken());
    verify(userRepository).save(user);
  }

  @Test
  @DisplayName("given a pending member and a valid token, when confirm then pending approval")
  void givenPendingMemberAndValidTokenWhenConfirmThenPendingApproval() {
    var user = IdentityFactory.pendingMember(UUID.randomUUID());
    when(userRepository.findByVerificationTokenHash(TOKEN_HASH)).thenReturn(Optional.of(user));
    when(passwordHasher.hash(PASSWORD)).thenReturn(PASSWORD_HASH);

    var status = service.confirm(new ConfirmRegistrationCommand(RAW_TOKEN, PASSWORD));

    assertEquals(UserStatus.PENDING_APPROVAL, status);
  }

  @Test
  @DisplayName(
      "given an unknown token, when confirm then rejected without hashing the password or saving")
  void givenUnknownTokenWhenConfirmThenRejectedWithoutHashingOrSaving() {
    when(userRepository.findByVerificationTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());
    var command = new ConfirmRegistrationCommand(RAW_TOKEN, PASSWORD);

    assertThrows(InvalidVerificationTokenException.class, () -> service.confirm(command));

    verifyNoInteractions(passwordHasher);
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("given an expired token, when confirm then rejected without saving")
  void givenExpiredTokenWhenConfirmThenRejectedWithoutSaving() {
    var expiredService =
        new ConfirmRegistrationService(userRepository, passwordHasher, CLOCK_AFTER_EXPIRY);
    var user = IdentityFactory.pendingOwner(UUID.randomUUID());
    when(userRepository.findByVerificationTokenHash(TOKEN_HASH)).thenReturn(Optional.of(user));
    var command = new ConfirmRegistrationCommand(RAW_TOKEN, PASSWORD);

    assertThrows(InvalidVerificationTokenException.class, () -> expiredService.confirm(command));

    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    verify(userRepository, never()).save(any());
  }
}
