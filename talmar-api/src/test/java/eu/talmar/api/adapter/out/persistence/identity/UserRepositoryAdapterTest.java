package eu.talmar.api.adapter.out.persistence.identity;

import static eu.talmar.api.testSupport.IdentityFactory.EMAIL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.talmar.api.adapter.out.persistence.identity.mapper.CompanyJpaMapperImpl;
import eu.talmar.api.adapter.out.persistence.identity.mapper.UserJpaMapperImpl;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
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
  @Autowired private TestEntityManager entityManager;

  private UUID companyId;

  @BeforeEach
  void setUp() {
    var company = IdentityFactory.company();
    companyAdapter.save(company);
    companyId = company.getId();
  }

  @Test
  @DisplayName("given a user, when saved and found by e-mail then every field round-trips")
  void givenUserWhenSavedAndFoundByEmailThenEveryFieldRoundTrips() {
    var user = IdentityFactory.owner(companyId);

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
    assertEquals(user.getStatus(), reloaded.getStatus());
  }

  @Test
  @DisplayName("given a saved user, when another user with the same e-mail is saved then rejected")
  void givenSavedUserWhenAnotherUserWithSameEmailIsSavedThenRejected() {
    adapter.save(IdentityFactory.owner(companyId));
    flushAndClear();
    adapter.save(IdentityFactory.member(companyId));

    assertThrows(DataIntegrityViolationException.class, () -> userJpaRepository.flush());
  }

  @Test
  @DisplayName(
      "given a user of a company that does not exist, when saved then the foreign key rejects it")
  void givenUserOfCompanyThatDoesNotExistWhenSavedThenTheForeignKeyRejectsIt() {
    adapter.save(IdentityFactory.owner(UUID.randomUUID()));

    assertThrows(DataIntegrityViolationException.class, () -> userJpaRepository.flush());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
