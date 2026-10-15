package app.jobzy.api.application.service;

import static app.jobzy.api.testSupport.IdentityFactory.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.jobzy.api.application.port.out.CompanyRepository;
import app.jobzy.api.application.port.out.UserRepository;
import app.jobzy.api.testSupport.IdentityFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeUnconfirmedRegistrationsServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);

  @Mock private UserRepository userRepository;
  @Mock private CompanyRepository companyRepository;

  private PurgeUnconfirmedRegistrationsService service;

  @BeforeEach
  void setUp() {
    service = new PurgeUnconfirmedRegistrationsService(userRepository, companyRepository, CLOCK);
  }

  @Test
  @DisplayName(
      "given an expired registration that was the only user of its company, when purged then user"
          + " and company are deleted")
  void givenExpiredRegistrationThatWasOnlyUserWhenPurgedThenUserAndCompanyAreDeleted() {
    var companyId = UUID.randomUUID();
    var user = IdentityFactory.pendingOwner(companyId);
    when(userRepository.findAwaitingVerificationExpiredAt(NOW)).thenReturn(List.of(user));
    when(userRepository.countByCompanyId(companyId)).thenReturn(0L);

    var purged = service.purgeUnconfirmedRegistrations();

    assertEquals(1, purged);
    verify(userRepository).delete(user);
    verify(companyRepository).deleteById(companyId);
  }

  @Test
  @DisplayName(
      "given an expired registration in a company with other users, when purged then only the"
          + " user is deleted")
  void givenExpiredRegistrationInCompanyWithOtherUsersWhenPurgedThenOnlyUserIsDeleted() {
    var companyId = UUID.randomUUID();
    var user = IdentityFactory.pendingMember(companyId);
    when(userRepository.findAwaitingVerificationExpiredAt(NOW)).thenReturn(List.of(user));
    when(userRepository.countByCompanyId(companyId)).thenReturn(1L);

    var purged = service.purgeUnconfirmedRegistrations();

    assertEquals(1, purged);
    verify(userRepository).delete(user);
    verify(companyRepository, never()).deleteById(any());
  }

  @Test
  @DisplayName("given no expired registrations, when purged then nothing is deleted")
  void givenNoExpiredRegistrationsWhenPurgedThenNothingIsDeleted() {
    when(userRepository.findAwaitingVerificationExpiredAt(NOW)).thenReturn(List.of());

    var purged = service.purgeUnconfirmedRegistrations();

    assertEquals(0, purged);
    verify(userRepository, never()).delete(any());
    verifyNoInteractions(companyRepository);
  }
}
