package eu.talmar.api.integration.vacancy;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.SalaryPeriod;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyResponse;
import io.restassured.http.ContentType;
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

/** Setting the contact person and offer of a vacancy ({@code PUT /vacancy/{id}/offer}). */
@Sql("/sql/existing-vacancy.sql")
class VacancyOfferIntegrationTest extends BaseIntegrationTest {

  /** Seeded by {@code sql/existing-vacancy.sql}: Sales Manager in Amsterdam. */
  private static final UUID VACANCY_ID = UUID.fromString("0199a000-0000-7000-8000-000000000011");

  private static final String PROBLEM_JSON = "application/problem+json";

  private static final String VALID_REQUEST =
      """
      {
        "contactPerson": {
          "name": "Łukasz Kowalski",
          "role": "Hiring manager",
          "phone": "0048 (0)601-234 567",
          "email": "Lukasz.Kowalski@acme.pl"
        },
        "offer": {
          "salaryMin": 3800.745,
          "salaryMax": 4600,
          "currency": "PLN",
          "salaryPeriod": "MONTHLY",
          "numberOfHolidays": 26
        }
      }
      """;

  @Test
  @DisplayName(
      "given an existing vacancy, when a valid contact person and offer are set then 200 with the"
          + " vacancy holding both, phone and e-mail normalised and salaries rounded to cents")
  void givenExistingVacancyWhenValidContactPersonAndOfferSetThen200WithBoth() {
    var vacancy = putOffer(VACANCY_ID, VALID_REQUEST, HttpStatus.OK);

    assertEquals(VACANCY_ID, vacancy.getId());
    assertEquals("Sales Manager", vacancy.getJobTitle());
    var contactPerson = vacancy.getContactPerson();
    assertEquals("Łukasz Kowalski", contactPerson.getName());
    assertEquals("Hiring manager", contactPerson.getRole());
    assertEquals("+48601234567", contactPerson.getPhone());
    assertEquals("lukasz.kowalski@acme.pl", contactPerson.getEmail());
    var offer = vacancy.getOffer();
    assertEquals(new BigDecimal("3800.75"), offer.getSalaryMin());
    assertEquals(new BigDecimal("4600.00"), offer.getSalaryMax());
    assertEquals(CurrencyCode.PLN, offer.getCurrency());
    assertEquals(SalaryPeriod.MONTHLY, offer.getSalaryPeriod());
    assertEquals(0, new BigDecimal("26").compareTo(offer.getNumberOfHolidays()));
  }

  @Test
  @DisplayName(
      "given a vacancy with a contact person and offer, when set again with fewer fields then the"
          + " left-out fields are cleared")
  void givenVacancyWithContactPersonAndOfferWhenSetAgainWithFewerFieldsThenLeftOutFieldsCleared() {
    putOffer(VACANCY_ID, VALID_REQUEST, HttpStatus.OK);
    var smallerRequest =
        """
        {
          "contactPerson": {"name": "Sanne de Vries", "email": "sanne@acme.nl"},
          "offer": {"numberOfHolidays": 25}
        }
        """;

    var vacancy = putOffer(VACANCY_ID, smallerRequest, HttpStatus.OK);

    assertEquals("Sanne de Vries", vacancy.getContactPerson().getName());
    assertNull(vacancy.getContactPerson().getRole());
    assertNull(vacancy.getContactPerson().getPhone());
    assertNull(vacancy.getOffer().getSalaryMin());
    assertNull(vacancy.getOffer().getCurrency());
  }

  @Test
  @DisplayName("given an unknown vacancy id, when a contact person and offer are set then 404")
  void givenUnknownVacancyIdWhenContactPersonAndOfferSetThen404() {
    var unknownId = UUID.fromString("0199a000-0000-7000-8000-0000000000ff");

    var response =
        given()
            .contentType(ContentType.JSON)
            .body(VALID_REQUEST)
            .when()
            .put("/vacancy/{id}/offer", unknownId)
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
  @DisplayName(
      "given a request that breaks the contract, when a contact person and offer are set then 400"
          + " naming the field")
  void givenRequestThatBreaksContractWhenSetThen400NamingTheField(
      String expectedField, String request) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(request)
            .when()
            .put("/vacancy/{id}/offer", VACANCY_ID)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals(expectedField, problemDetails.getErrors().getFirst().getField());
  }

  static Stream<Arguments> invalidRequests() {
    var validContactPerson = "{\"name\": \"Sanne de Vries\", \"email\": \"sanne@acme.nl\"}";
    var validOffer = "{\"numberOfHolidays\": 25}";
    return Stream.of(
        Arguments.of("offer", "{\"contactPerson\": %s}".formatted(validContactPerson)),
        Arguments.of("contactPerson", "{\"offer\": %s}".formatted(validOffer)),
        Arguments.of(
            "contactPerson.name",
            request("{\"name\": \"Sanne <b>\", \"email\": \"sanne@acme.nl\"}", validOffer)),
        Arguments.of(
            "contactPerson.email",
            request("{\"name\": \"Sanne de Vries\", \"email\": \"not-an-address\"}", validOffer)),
        Arguments.of(
            "contactPerson.phone",
            request(
                "{\"name\": \"Sanne de Vries\", \"email\": \"sanne@acme.nl\", \"phone\":"
                    + " \"call me\"}",
                validOffer)),
        Arguments.of("offer", request(validContactPerson, "{}")),
        Arguments.of(
            "offer.currency",
            request(validContactPerson, "{\"salaryMin\": 3800, \"salaryPeriod\": \"MONTHLY\"}")),
        Arguments.of(
            "offer.salaryPeriod",
            request(validContactPerson, "{\"salaryMin\": 3800, \"currency\": \"EUR\"}")),
        Arguments.of(
            "offer.salaryMin",
            request(
                validContactPerson,
                "{\"salaryMin\": 4600.01, \"salaryMax\": 4600, \"currency\": \"EUR\","
                    + " \"salaryPeriod\": \"MONTHLY\"}")),
        Arguments.of(
            "offer.numberOfHolidays", request(validContactPerson, "{\"numberOfHolidays\": 61}")));
  }

  @Test
  @DisplayName("given a currency outside Europe, when a contact person and offer are set then 400")
  void givenCurrencyOutsideEuropeWhenSetThen400() {
    var request =
        request(
            "{\"name\": \"Sanne de Vries\", \"email\": \"sanne@acme.nl\"}",
            "{\"salaryMin\": 3800, \"currency\": \"USD\", \"salaryPeriod\": \"MONTHLY\"}");

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/vacancy/{id}/offer", VACANCY_ID)
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .contentType(PROBLEM_JSON);
  }

  private static String request(String contactPerson, String offer) {
    return "{\"contactPerson\": %s, \"offer\": %s}".formatted(contactPerson, offer);
  }

  private static VacancyResponse putOffer(UUID vacancyId, String body, HttpStatus expectedStatus) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .put("/vacancy/{id}/offer", vacancyId)
            .then()
            .statusCode(expectedStatus.value())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, VacancyResponse.class);
  }
}
