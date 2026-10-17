package eu.talmar.api.adapter.out.persistence.identity;

import static eu.talmar.api.testSupport.IdentityFactory.EMAIL;
import static eu.talmar.api.testSupport.IdentityFactory.NOW;
import static eu.talmar.api.testSupport.IdentityFactory.PASSWORD_HASH;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_EXPIRES_AT;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_HASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.adapter.out.persistence.identity.mapper.CompanyJpaMapperImpl;
import eu.talmar.api.adapter.out.persistence.identity.mapper.UserJpaMapperImpl;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.testSupport.IdentityFactory;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import({
  UserRepositoryAdapter.class,
  UserJpaMapperImpl.class,
  CompanyRepositoryAdapter.class,
  CompanyJpaMapperImpl.class
})
class UserRepositoryAdapterTest {
  @Autowired private UserRepositoryAdapter adapter;
  @Autowired private CompanyRepositoryAdapter companyAdapter;
  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private UserCredentialsJpaRepository credentialsJpaRepository;
  @Autowired private TestEntityManager entityManager;

  private UUID companyId;

  @BeforeEach
  void setUp() {
    var company = IdentityFactory.company();
    companyAdapter.save(company);
    companyId = company.getId();
  }

  @Test
  @DisplayName(
      "given a pending user, when saved and found by e-mail then every field round-trips and no"
          + " credentials exist")
  void givenPendingUserWhenSavedAndFoundByEmailThenEveryFieldRoundTrips() {
    var user = IdentityFactory.pendingOwner(companyId);

    adapter.save(user);
    flushAndClear();
    var reloaded = adapter.findByEmail(new EmailAddress(EMAIL)).orElseThrow();

    assertEquals(user.getId(), reloaded.getId());
    assertEquals(user.getFirstName(), reloaded.getFirstName());
    assertEquals(user.getLastName(), reloaded.getLastName());
    assertEquals(user.getEmail(), reloaded.getEmail());
    assertEquals(user.getJobRole(), reloaded.getJobRole());
    assertEquals(companyId, reloaded.getCompanyId());
    assertEquals(user.getMembership(), reloaded.getMembership());
    assertEquals(UserStatus.PENDING_VERIFICATION, reloaded.getStatus());
    assertEquals(IdentityFactory.validToken(), reloaded.getVerificationToken());
    assertNull(reloaded.getCredentials());
    assertEquals(0, credentialsJpaRepository.count());
  }

  @Test
  @DisplayName(
      "given a confirmed user, when saved then the credentials are stored in their own row and the"
          + " token is gone")
  void givenConfirmedUserWhenSavedThenCredentialsAreStoredInOwnRowAndTokenIsGone() {
    var user = IdentityFactory.pendingOwner(companyId);
    adapter.save(user);
    user.confirm(TOKEN_HASH, new UserCredentials(PASSWORD_HASH), NOW);

    adapter.save(user);
    flushAndClear();

    var reloaded = adapter.findByEmail(new EmailAddress(EMAIL)).orElseThrow();
    assertEquals(UserStatus.ACTIVE, reloaded.getStatus());
    assertEquals(new UserCredentials(PASSWORD_HASH), reloaded.getCredentials());
    assertNull(reloaded.getVerificationToken());
    assertEquals(
        PASSWORD_HASH,
        credentialsJpaRepository.findById(user.getId()).orElseThrow().getPasswordHash());
    assertTrue(adapter.findByVerificationTokenHash(TOKEN_HASH).isEmpty());
  }

  @Test
  @DisplayName("given a saved user, when found by token hash then that user is returned")
  void givenSavedUserWhenFoundByTokenHashThenThatUserIsReturned() {
    var user = IdentityFactory.pendingOwner(companyId);
    adapter.save(user);
    flushAndClear();

    assertEquals(user, adapter.findByVerificationTokenHash(TOKEN_HASH).orElseThrow());
  }

  @Test
  @DisplayName(
      "given pending and confirmed users, when looking for expired ones then only pending users"
          + " whose token expired are found")
  void givenPendingAndConfirmedUsersWhenLookingForExpiredOnesThenOnlyExpiredPendingAreFound() {
    var expired = IdentityFactory.pendingOwner(companyId);
    adapter.save(expired);
    adapter.save(IdentityFactory.activeOwner(companyId, "active@acme.nl"));
    flushAndClear();

    assertTrue(
        adapter.findAwaitingVerificationExpiredAt(TOKEN_EXPIRES_AT.minusSeconds(1)).isEmpty());
    var found = adapter.findAwaitingVerificationExpiredAt(TOKEN_EXPIRES_AT);
    assertEquals(1, found.size());
    assertEquals(expired, found.getFirst());
  }

  @Test
  @DisplayName("given users in a company, when counted then the count is per company")
  void givenUsersInCompanyWhenCountedThenTheCountIsPerCompany() {
    adapter.save(IdentityFactory.pendingOwner(companyId));
    flushAndClear();

    assertEquals(1, adapter.countByCompanyId(companyId));
    assertEquals(0, adapter.countByCompanyId(UUID.randomUUID()));
  }

  @Test
  @DisplayName("given a saved user, when deleted then the row is gone")
  void givenSavedUserWhenDeletedThenTheRowIsGone() {
    var user = IdentityFactory.pendingOwner(companyId);
    adapter.save(user);
    flushAndClear();

    adapter.delete(user);
    flushAndClear();

    assertEquals(0, userJpaRepository.count());
  }

  @Test
  @DisplayName("given a saved user, when another user with the same e-mail is saved then rejected")
  void givenSavedUserWhenAnotherUserWithSameEmailIsSavedThenRejected() {
    adapter.save(IdentityFactory.pendingOwner(companyId));
    flushAndClear();
    adapter.save(IdentityFactory.pendingMember(companyId));

    assertThrows(DataIntegrityViolationException.class, () -> userJpaRepository.flush());
  }

  @Test
  @DisplayName(
      "given a user of a company that does not exist, when saved then the foreign key rejects it")
  void givenUserOfCompanyThatDoesNotExistWhenSavedThenTheForeignKeyRejectsIt() {
    adapter.save(IdentityFactory.pendingOwner(UUID.randomUUID()));

    assertThrows(DataIntegrityViolationException.class, () -> userJpaRepository.flush());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
