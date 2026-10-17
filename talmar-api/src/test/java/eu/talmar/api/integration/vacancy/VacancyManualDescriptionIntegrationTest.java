package eu.talmar.api.integration.vacancy;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyDescriptionJpaRepository;
import eu.talmar.api.adapter.out.persistence.vacancy.VacancyJpaRepository;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionResponse;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyResponse;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Runs against an in-memory H2 database, exercising {@code POST /vacancy/{id}/description}
 * end-to-end through the real REST, application and persistence layers.
 */
class VacancyManualDescriptionIntegrationTest extends BaseIntegrationTest {

  private static final String PROBLEM_JSON = "application/problem+json";

  private static final String VALID_DESCRIPTION_REQUEST =
      """
      {
        "summary": "Summary",
        "jobDescription": "Job description",
        "tasks": "Tasks",
        "whatWeOffer": "What we offer",
        "aboutUs": "About us"
      }
      """;

  @Autowired private VacancyJpaRepository vacancyJpaRepository;
  @Autowired private VacancyDescriptionJpaRepository vacancyDescriptionJpaRepository;

  @BeforeEach
  void setUp() {
    vacancyDescriptionJpaRepository.deleteAll();
    vacancyJpaRepository.deleteAll();
  }

  @Test
  @DisplayName(
      "given an existing vacancy, when a valid description is submitted then it is persisted with"
          + " source MANUAL")
  void givenExistingVacancyWhenValidDescriptionSubmittedThenPersistedWithSourceManual() {
    var vacancyId = createVacancy();

    var response = postDescription(vacancyId, VALID_DESCRIPTION_REQUEST, HttpStatus.CREATED);
    var body = JSON_MAPPER.readValue(response, VacancyDescriptionResponse.class);

    assertEquals("Summary", body.getSummary());
    assertEquals("Job description", body.getJobDescription());
    assertEquals("Tasks", body.getTasks());
    assertEquals("What we offer", body.getWhatWeOffer());
    assertEquals("About us", body.getAboutUs());

    var persisted = vacancyDescriptionJpaRepository.findByVacancyId(vacancyId).orElseThrow();
    assertEquals(VacancyDescriptionSource.MANUAL, persisted.getSource());
  }

  @Test
  @DisplayName(
      "given a vacancy with an existing description, when a different description is submitted"
          + " then it replaces the old one without creating a duplicate row")
  void
      givenVacancyWithExistingDescriptionWhenDifferentDescriptionSubmittedThenReplacesWithoutDuplicateRow() {
    var vacancyId = createVacancy();
    postDescription(vacancyId, VALID_DESCRIPTION_REQUEST, HttpStatus.CREATED);

    var secondRequest =
        """
        {
          "summary": "New summary"
        }
        """;
    postDescription(vacancyId, secondRequest, HttpStatus.CREATED);

    var persisted = vacancyDescriptionJpaRepository.findByVacancyId(vacancyId).orElseThrow();
    assertEquals("New summary", persisted.getSummary());
    assertEquals(1, vacancyDescriptionJpaRepository.count());
  }

  @Test
  @DisplayName(
      "given raw HTML content, when submitted then rejected with 400 and nothing is persisted")
  void givenRawHtmlContentWhenSubmittedThenRejectedAndNothingPersisted() {
    var vacancyId = createVacancy();
    var invalidRequest =
        """
        {
          "summary": "<script>alert(1)</script>"
        }
        """;

    var response = postDescription(vacancyId, invalidRequest, HttpStatus.BAD_REQUEST);
    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals("summary", problemDetails.getErrors().getFirst().getField());
    assertTrue(vacancyDescriptionJpaRepository.findByVacancyId(vacancyId).isEmpty());
  }

  @Test
  @DisplayName(
      "given content exceeding a field's max length, when submitted then rejected with 400")
  void givenContentExceedingMaxLengthWhenSubmittedThenRejectedWith400() {
    var vacancyId = createVacancy();
    var tooLong = "\"" + "a".repeat(1001) + "\"";
    var invalidRequest = "{\"summary\": %s}".formatted(tooLong);

    var response = postDescription(vacancyId, invalidRequest, HttpStatus.BAD_REQUEST);
    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals("summary", problemDetails.getErrors().getFirst().getField());
    assertTrue(vacancyDescriptionJpaRepository.findByVacancyId(vacancyId).isEmpty());
  }

  @Test
  @DisplayName("given an unknown vacancy id, when submitted then rejected with 404")
  void givenUnknownVacancyIdWhenSubmittedThenRejectedWith404() {
    postDescription(UUID.randomUUID(), VALID_DESCRIPTION_REQUEST, HttpStatus.NOT_FOUND);
  }

  private UUID createVacancy() {
    var requestBody =
        """
        {
          "jobTitle": "Sales Manager",
          "language": "NL",
          "category": "SALES",
          "location": {"country": "NL", "city": "Amsterdam"},
          "workplaceType": "HYBRID",
          "minHoursPerWeek": 32,
          "maxHoursPerWeek": 40
        }
        """;
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/vacancy")
            .then()
            .statusCode(HttpStatus.CREATED.value())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, VacancyResponse.class).getId();
  }

  private String postDescription(UUID vacancyId, String requestBody, HttpStatus expectedStatus) {
    return given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .when()
        .post("/vacancy/{id}/description", vacancyId)
        .then()
        .statusCode(expectedStatus.value())
        .contentType(expectedStatus.isError() ? PROBLEM_JSON : ContentType.JSON.toString())
        .extract()
        .asString();
  }
}
