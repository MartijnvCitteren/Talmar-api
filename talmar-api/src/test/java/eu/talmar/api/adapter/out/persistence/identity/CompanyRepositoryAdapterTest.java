package eu.talmar.api.adapter.out.persistence.identity;

import static eu.talmar.api.testSupport.IdentityFactory.EMAIL_DOMAIN;
import static eu.talmar.api.testSupport.IdentityFactory.FOUNDER_ID;
import static eu.talmar.api.testSupport.IdentityFactory.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.adapter.out.persistence.identity.mapper.CompanyJpaMapperImpl;
import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.testSupport.IdentityFactory;
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
@Import({CompanyRepositoryAdapter.class, CompanyJpaMapperImpl.class})
class CompanyRepositoryAdapterTest {

  @Autowired private CompanyRepositoryAdapter adapter;
  @Autowired private CompanyJpaRepository companyJpaRepository;
  @Autowired private TestEntityManager entityManager;

  @Test
  @DisplayName("given a saved company, when found by e-mail domain then every field round-trips")
  void givenSavedCompanyWhenFoundByEmailDomainThenEveryFieldRoundTrips() {
    var company = IdentityFactory.company();

    adapter.save(company);
    flushAndClear();
    var reloaded = adapter.findByEmailDomain(EMAIL_DOMAIN).orElseThrow();

    assertEquals(company.getId(), reloaded.getId());
    assertEquals(company.getName(), reloaded.getName());
    assertEquals(company.getEmailDomain(), reloaded.getEmailDomain());
    assertEquals(company.getLastModifiedBy(), reloaded.getLastModifiedBy());
  }

  @Test
  @DisplayName("given an unknown e-mail domain, when found then empty")
  void givenUnknownEmailDomainWhenFoundThenEmpty() {
    assertTrue(adapter.findByEmailDomain("unknown.nl").isEmpty());
  }

  @Test
  @DisplayName("given a saved company, when deleted by id then the row is gone")
  void givenSavedCompanyWhenDeletedByIdThenTheRowIsGone() {
    var company = IdentityFactory.company();
    adapter.save(company);
    flushAndClear();

    adapter.deleteById(company.getId());
    flushAndClear();

    assertEquals(0, companyJpaRepository.count());
  }

  @Test
  @DisplayName(
      "given a company for a domain, when a second company for the same domain is saved then"
          + " rejected")
  void givenCompanyForDomainWhenSecondCompanyForSameDomainIsSavedThenRejected() {
    adapter.save(IdentityFactory.company());
    flushAndClear();
    adapter.save(Company.found(EMAIL_DOMAIN, FOUNDER_ID, NOW));

    assertThrows(DataIntegrityViolationException.class, () -> companyJpaRepository.flush());
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}
