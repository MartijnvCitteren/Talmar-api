package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.IdentityFactory.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.ChangeNameCommand;
import eu.talmar.api.application.port.out.IdentityProvider;
import eu.talmar.api.application.port.out.IdentityProviderUnavailableException;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.UserNotOnboardedException;
import eu.talmar.api.shared.Constants;
import eu.talmar.api.testSupport.IdentityFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeNameServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneId.of(Constants.AMS_TIME_ZONE_ID));
  private static final UUID COMPANY_ID = UUID.randomUUID();

  @Mock private UserRepository userRepository;
  @Mock private IdentityProvider identityProvider;
  @Spy private Clock clock = CLOCK;

  @InjectMocks private ChangeNameService service;

  @Test
  @DisplayName(
      "given both names, when changed then the identity provider gets them first and the copy is"
          + " saved with them")
  void givenBothNamesWhenChangedThenIdentityProviderGetsThemAndCopyIsSaved() {
    var user = IdentityFactory.owner(COMPANY_ID);
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(user));
    var firstName = "José";
    var lastName = "'t Hooft";

    service.changeName(new ChangeNameCommand(SUBJECT, firstName, lastName));

    verify(identityProvider).changeName(SUBJECT, firstName, lastName);
    verify(userRepository).save(user);
    assertEquals(firstName, user.getFirstName());
    assertEquals(lastName, user.getLastName());
    assertEquals(LocalDateTime.now(clock), user.getLastModifiedAt());
  }

  @Test
  @DisplayName(
      "given only a last name, when changed then the identity provider also gets the current first"
          + " name")
  void givenOnlyLastNameWhenChangedThenIdentityProviderAlsoGetsCurrentFirstName() {
    var user = IdentityFactory.owner(COMPANY_ID);
    var currentFirstName = user.getFirstName();
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(user));
    var lastName = "van den Berg";

    service.changeName(new ChangeNameCommand(SUBJECT, null, lastName));

    verify(identityProvider).changeName(SUBJECT, currentFirstName, lastName);
    assertEquals(currentFirstName, user.getFirstName());
  }

  @Test
  @DisplayName("given no names, when changed then nothing is called or saved")
  void givenNoNamesWhenChangedThenNothingIsCalledOrSaved() {
    when(userRepository.findBySubject(SUBJECT))
        .thenReturn(Optional.of(IdentityFactory.owner(COMPANY_ID)));

    service.changeName(new ChangeNameCommand(SUBJECT, null, null));

    verify(identityProvider, never()).changeName(anyString(), anyString(), anyString());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName("given an account without user, when changed then refused and Keycloak not called")
  void givenAccountWithoutUserWhenChangedThenRefusedAndKeycloakNotCalled() {
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.empty());
    var command = new ChangeNameCommand(SUBJECT, "Jane", null);

    assertThrows(UserNotOnboardedException.class, () -> service.changeName(command));

    verify(identityProvider, never()).changeName(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("given the identity provider fails, when changed then the copy is not changed")
  void givenIdentityProviderFailsWhenChangedThenCopyIsNotChanged() {
    var user = IdentityFactory.owner(COMPANY_ID);
    var originalLastName = user.getLastName();
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(user));
    doThrow(new IdentityProviderUnavailableException("change name"))
        .when(identityProvider)
        .changeName(anyString(), anyString(), anyString());
    var command = new ChangeNameCommand(SUBJECT, null, "van den Berg");

    assertThrows(IdentityProviderUnavailableException.class, () -> service.changeName(command));

    verify(userRepository, never()).save(any());
    assertEquals(originalLastName, user.getLastName());
  }
}
