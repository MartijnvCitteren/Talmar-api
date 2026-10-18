package eu.talmar.api.domain.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompanyTest {
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 9, 12, 0);
  private static final UUID FOUNDER_ID = UUID.randomUUID();

  @Test
  @DisplayName(
      "given an e-mail domain and a founder, when founded then the company has that domain, records"
          + " the founder as last modifier and gets an id")
  void givenEmailDomainAndFounderWhenFoundedThenCompanyRecordsFounderAsLastModifier() {
    var company = Company.found("acme.nl", FOUNDER_ID, NOW);

    assertNotNull(company.getId());
    assertEquals("acme.nl", company.getEmailDomain());
    assertEquals(FOUNDER_ID, company.getLastModifiedBy());
    assertEquals(NOW, company.getCreatedAt());
    assertEquals(NOW, company.getLastModifiedAt());
  }

  @ParameterizedTest
  @CsvSource({
    "acme.nl, Acme",
    "sales.acme.nl, Acme",
    "acme.co.uk, Acme",
    "acme.com.au, Acme",
    "acme-logistics.nl, Acme-logistics",
    "talmar.eu, Talmar"
  })
  @DisplayName(
      "given an e-mail domain, when founded then the name is its registrable label, capitalised")
  void givenEmailDomainWhenFoundedThenNameIsItsRegistrableLabelCapitalised(
      String emailDomain, String expectedName) {
    assertEquals(expectedName, Company.found(emailDomain, FOUNDER_ID, NOW).getName());
  }

  @Test
  @DisplayName("given a builder without last modifier, when built then throws naming the field")
  void givenBuilderWithoutLastModifierWhenBuiltThenThrowsNamingTheField() {
    var builder = Company.builder().name("Acme").emailDomain("acme.nl").createdAt(NOW);

    var exception = assertThrows(NullPointerException.class, builder::build);

    assertTrue(exception.getMessage().contains("lastModifiedBy"));
  }

  @Test
  @DisplayName("given a builder with an explicit id, when built then the company has that id")
  void givenBuilderWithExplicitIdWhenBuiltThenTheCompanyHasThatId() {
    var id = UUID.randomUUID();

    var company =
        Company.builder()
            .id(id)
            .name("Acme")
            .emailDomain("acme.nl")
            .lastModifiedBy(FOUNDER_ID)
            .createdAt(NOW)
            .build();

    assertEquals(id, company.getId());
  }

  @Test
  @DisplayName("given two companies, when compared then equality follows the id")
  void givenTwoCompaniesWhenComparedThenEqualityFollowsTheId() {
    var id = UUID.randomUUID();
    var company =
        Company.builder()
            .id(id)
            .name("Acme")
            .emailDomain("acme.nl")
            .lastModifiedBy(FOUNDER_ID)
            .createdAt(NOW);

    assertEquals(company.build(), company.build());
    assertEquals(company.build().hashCode(), company.build().hashCode());
    assertNotEquals(
        Company.found("acme.nl", FOUNDER_ID, NOW), Company.found("acme.nl", FOUNDER_ID, NOW));
  }
}
