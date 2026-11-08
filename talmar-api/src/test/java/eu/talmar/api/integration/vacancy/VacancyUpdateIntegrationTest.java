package eu.talmar.api.integration.vacancy;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.Language;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.SalaryPeriod;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyCategory;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyResponse;
import eu.talmar.api.vacancy.adapter.in.web.contract.WorkplaceType;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/** Partially updating a vacancy ({@code PATCH /vacancy/{id}}). */
@Sql("/sql/existing-vacancy.sql")
class VacancyUpdateIntegrationTest extends BaseIntegrationTest {

  /** Seeded by {@code sql/existing-vacancy.sql}: Sales Manager in Amsterdam, NL, 32-40 hours. */
  private static final UUID VACANCY_ID = UUID.fromString("0199a000-0000-7000-8000-000000000011");

  private static final String MERGE_PATCH_JSON = "application/merge-patch+json";
  private static final String PROBLEM_JSON = "application/problem+json";

  @Test
  @DisplayName(
      "given an existing vacancy, when only the job title is sent then 200 with the new job title"
          + " and every other field as it was")
  void givenExistingVacancyWhenOnlyJobTitleSentThen200WithOtherFieldsAsTheyWere() {
    var vacancy = patch(VACANCY_ID, "{\"jobTitle\": \"Senior Sales Manager\"}");

    assertEquals("Senior Sales Manager", vacancy.getJobTitle());
    assertEquals(Language.NL, vacancy.getLanguage());
    assertEquals(VacancyCategory.SALES, vacancy.getCategory());
    assertEquals("Amsterdam", vacancy.getLocation().getCity());
    assertEquals(WorkplaceType.HYBRID, vacancy.getWorkplaceType());
    assertEquals(0, new BigDecimal("32").compareTo(vacancy.getMinHoursPerWeek()));
  }

  @Test
  @DisplayName("given an existing vacancy, when every section is sent then 200 with all of them")
  void givenExistingVacancyWhenEverySectionSentThen200WithAllOfThem() {
    var request =
        """
        {
          "jobTitle": "Verkaufsleiter",
          "language": "DE",
          "category": "MANAGEMENT",
          "location": {"country": "DE", "city": "Berlin"},
          "workplaceType": "REMOTE",
          "minHoursPerWeek": 36,
          "maxHoursPerWeek": 38.5,
          "description": {"summary": "Leite unser Vertriebsteam.", "tasks": "Planen"},
          "contactPerson": {"name": "Sanne de Vries", "email": "Sanne@Acme.nl"},
          "offer": {
            "salaryMin": 4200,
            "salaryMax": 5000,
            "currency": "EUR",
            "salaryPeriod": "MONTHLY"
          }
        }
        """;

    var vacancy = patch(VACANCY_ID, request);

    assertEquals("Verkaufsleiter", vacancy.getJobTitle());
    assertEquals(Language.DE, vacancy.getLanguage());
    assertEquals(VacancyCategory.MANAGEMENT, vacancy.getCategory());
    assertEquals("DE", vacancy.getLocation().getCountry());
    assertEquals("Berlin", vacancy.getLocation().getCity());
    assertEquals(WorkplaceType.REMOTE, vacancy.getWorkplaceType());
    assertEquals(0, new BigDecimal("36").compareTo(vacancy.getMinHoursPerWeek()));
    assertEquals(0, new BigDecimal("38.5").compareTo(vacancy.getMaxHoursPerWeek()));
    assertEquals("Leite unser Vertriebsteam.", vacancy.getDescription().getSummary());
    assertEquals("Planen", vacancy.getDescription().getTasks());
    assertEquals("Sanne de Vries", vacancy.getContactPerson().getName());
    assertEquals("sanne@acme.nl", vacancy.getContactPerson().getEmail());
    assertEquals(new BigDecimal("4200.00"), vacancy.getOffer().getSalaryMin());
    assertEquals(CurrencyCode.EUR, vacancy.getOffer().getCurrency());
    assertEquals(SalaryPeriod.MONTHLY, vacancy.getOffer().getSalaryPeriod());
  }

  @Test
  @DisplayName(
      "given a vacancy with a contact person and offer, when only a smaller offer is sent then the"
          + " offer is replaced as a whole and the contact person stays")
  void givenVacancyWithContactPersonAndOfferWhenSmallerOfferSentThenOfferReplacedAndContactStays() {
    patch(
        VACANCY_ID,
        """
        {
          "contactPerson": {"name": "Sanne de Vries", "email": "sanne@acme.nl"},
          "offer": {"salaryMin": 4200, "currency": "EUR", "salaryPeriod": "MONTHLY"}
        }
        """);

    var vacancy = patch(VACANCY_ID, "{\"offer\": {\"numberOfHolidays\": 25}}");

    assertEquals("Sanne de Vries", vacancy.getContactPerson().getName());
    assertEquals(0, new BigDecimal("25").compareTo(vacancy.getOffer().getNumberOfHolidays()));
    assertNull(vacancy.getOffer().getSalaryMin());
    assertNull(vacancy.getOffer().getCurrency());
  }

  @Test
  @DisplayName("given an existing vacancy, when a field is sent as null then it stays as it was")
  void givenExistingVacancyWhenFieldSentAsNullThenItStaysAsItWas() {
    var vacancy = patch(VACANCY_ID, "{\"jobTitle\": null, \"location\": null}");

    assertEquals("Sales Manager", vacancy.getJobTitle());
    assertEquals("Amsterdam", vacancy.getLocation().getCity());
  }

  @Test
  @DisplayName("given an unknown vacancy id, when updated then 404")
  void givenUnknownVacancyIdWhenUpdatedThen404() {
    var unknownId = UUID.fromString("0199a000-0000-7000-8000-0000000000ff");

    var response =
        given()
            .contentType(MERGE_PATCH_JSON)
            .body("{\"jobTitle\": \"Chef\"}")
            .when()
            .patch("/vacancy/{id}", unknownId)
            .then()
            .statusCode(HttpStatus.NOT_FOUND.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals(HttpStatus.NOT_FOUND.value(), problemDetails.getStatus());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  @DisplayName("given a request that breaks the contract, when updated then 400 naming the field")
  void givenRequestThatBreaksContractWhenUpdatedThen400NamingTheField(
      String expectedField, String request) {
    var response =
        given()
            .contentType(MERGE_PATCH_JSON)
            .body(request)
            .when()
            .patch("/vacancy/{id}", VACANCY_ID)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals(expectedField, problemDetails.getErrors().getFirst().getField());
  }

  static Stream<Arguments> invalidRequests() {
    return Stream.of(
        Arguments.of("maxHoursPerWeek", "{\"minHoursPerWeek\": 32}"),
        Arguments.of("minHoursPerWeek", "{\"maxHoursPerWeek\": 40}"),
        Arguments.of("maxHoursPerWeek", "{\"minHoursPerWeek\": 32, \"maxHoursPerWeek\": 61}"),
        Arguments.of("location.country", "{\"location\": {\"country\": \"nl\", \"city\": \"A\"}}"),
        Arguments.of("description.summary", "{\"description\": {\"summary\": \"<b>Hi</b>\"}}"),
        Arguments.of(
            "contactPerson.email",
            "{\"contactPerson\": {\"name\": \"Sanne de Vries\", \"email\": \"not-an-address\"}}"),
        Arguments.of("offer", "{\"offer\": {}}"),
        Arguments.of(
            "offer.currency", "{\"offer\": {\"salaryMin\": 3800, \"salaryPeriod\": \"MONTHLY\"}}"));
  }

  private static VacancyResponse patch(UUID vacancyId, String body) {
    var response =
        given()
            .contentType(MERGE_PATCH_JSON)
            .body(body)
            .when()
            .patch("/vacancy/{id}", vacancyId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, VacancyResponse.class);
  }
}
