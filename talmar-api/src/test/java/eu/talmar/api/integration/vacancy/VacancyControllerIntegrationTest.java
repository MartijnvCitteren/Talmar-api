package eu.talmar.api.integration.vacancy;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyJpaRepository;
import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.Language;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetailsErrorsInner;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyResponse;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyStatus;
import io.restassured.http.ContentType;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

/**
 * Runs against an in-memory H2 database so the vacancy is persisted through the real JPA adapter.
 */
class VacancyControllerIntegrationTest extends BaseIntegrationTest {

  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String TOO_LONG = "\"" + "a".repeat(201) + "\"";

  /** The core request as raw JSON values, valid according to the contract. */
  private static final Map<String, String> VALID_CORE_REQUEST =
      Map.ofEntries(
          Map.entry("jobTitle", "\"Sales Manager\""),
          Map.entry("language", "\"NL\""),
          Map.entry("category", "\"SALES\""),
          Map.entry("location", "{\"country\": \"NL\", \"city\": \"Amsterdam\"}"),
          Map.entry("workplaceType", "\"HYBRID\""),
          Map.entry("minHoursPerWeek", "32"),
          Map.entry("maxHoursPerWeek", "40"));

  @Autowired private VacancyJpaRepository vacancyJpaRepository;

  @BeforeEach
  void setUp() {
    vacancyJpaRepository.deleteAll();
  }

  @Test
  @DisplayName("POST /vacancy creates a vacancy in DRAFT status, persists it and returns it")
  void givenValidCoreRequestWhenCreateVacancyThenReturnsCreatedVacancyInDraft() {
    var result =
        makeRequestAndExpectedStatus(
            requestBody(Map.of()), HttpStatus.CREATED, VacancyResponse.class);

    assertInstanceOf(UUID.class, result.getId());
    assertEquals(VacancyStatus.DRAFT, result.getStatus());
    assertEquals("Sales Manager", result.getJobTitle());
    assertEquals(Language.NL, result.getLanguage());
    assertEquals("NL", result.getLocation().getCountry());
    assertEquals("Amsterdam", result.getLocation().getCity());
    assertEquals(BigDecimal.valueOf(32), result.getMinHoursPerWeek());
    assertEquals(BigDecimal.valueOf(40), result.getMaxHoursPerWeek());
    assertNotNull(result.getCreatedAt());

    assertTrue(vacancyJpaRepository.findById(result.getId()).isPresent());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("contractViolations")
  @DisplayName(
      "given a core request violating the contract, when create vacancy, then it is rejected with a"
          + " problem detail naming the offending field")
  void givenContractViolationWhenCreateVacancyThenRejectedWithProblemDetailNamingTheField(
      String violation, String expectedField, String jsonField, String jsonValue) {
    var problemDetails =
        makeRequestAndExpectedStatus(
            requestBody(Map.of(jsonField, jsonValue)),
            HttpStatus.BAD_REQUEST,
            ProblemDetails.class);

    assertEquals("Validation failed", problemDetails.getTitle());
    assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetails.getStatus());
    assertEquals(
        List.of(expectedField),
        problemDetails.getErrors().stream().map(ProblemDetailsErrorsInner::getField).toList());
    assertNotNull(problemDetails.getErrors().getFirst().getMessage());
    assertEquals(0, vacancyJpaRepository.count());
  }

  private static Stream<Arguments> contractViolations() {
    return Stream.of(
        arguments("jobTitle is missing", "jobTitle", "jobTitle", "null"),
        arguments("jobTitle exceeds 200 characters", "jobTitle", "jobTitle", TOO_LONG),
        arguments("language is missing", "language", "language", "null"),
        arguments("category is missing", "category", "category", "null"),
        arguments("location is missing", "location", "location", "null"),
        arguments(
            "location.country is missing",
            "location.country",
            "location",
            "{\"city\": \"Leiden\"}"),
        arguments(
            "location.country is not an ISO 3166-1 alpha-2 code",
            "location.country",
            "location",
            "{\"country\": \"NLD\", \"city\": \"Leiden\"}"),
        arguments("location.city is missing", "location.city", "location", "{\"country\": \"NL\"}"),
        arguments(
            "location.city exceeds 200 characters",
            "location.city",
            "location",
            "{\"country\": \"NL\", \"city\": " + TOO_LONG + "}"),
        arguments("workplaceType is missing", "workplaceType", "workplaceType", "null"),
        arguments("minHoursPerWeek is missing", "minHoursPerWeek", "minHoursPerWeek", "null"),
        arguments("minHoursPerWeek is less than 0", "minHoursPerWeek", "minHoursPerWeek", "-1"),
        arguments("minHoursPerWeek exceeds 60", "minHoursPerWeek", "minHoursPerWeek", "61"),
        arguments("maxHoursPerWeek is missing", "maxHoursPerWeek", "maxHoursPerWeek", "null"),
        arguments(
            "maxHoursPerWeek is not greater than 1", "maxHoursPerWeek", "maxHoursPerWeek", "1"),
        arguments("maxHoursPerWeek exceeds 60", "maxHoursPerWeek", "maxHoursPerWeek", "61"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unreadableRequests")
  @DisplayName(
      "given a core request with a value outside an enum, when create vacancy, then it is rejected"
          + " with 400")
  void givenValueOutsideEnumWhenCreateVacancyThenRejectedWith400(
      String violation, String jsonField, String jsonValue) {
    var problemDetails =
        makeRequestAndExpectedStatus(
            requestBody(Map.of(jsonField, jsonValue)),
            HttpStatus.BAD_REQUEST,
            ProblemDetails.class);

    assertEquals("Validation failed", problemDetails.getTitle());
    assertEquals(0, vacancyJpaRepository.count());
  }

  private static Stream<Arguments> unreadableRequests() {
    return Stream.of(
        arguments("language in lower case", "language", "\"nl\""),
        arguments("unknown category", "category", "\"ASTRONAUT\""));
  }

  private String requestBody(Map<String, String> overrides) {
    var fields = new LinkedHashMap<>(VALID_CORE_REQUEST);
    fields.putAll(overrides);
    return fields.entrySet().stream()
        .map(field -> "\"%s\": %s".formatted(field.getKey(), field.getValue()))
        .collect(Collectors.joining(",\n", "{\n", "\n}"));
  }

  private <T> T makeRequestAndExpectedStatus(
      String requestBody, HttpStatus status, Class<T> responseType) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/vacancy")
            .then()
            .statusCode(status.value())
            .contentType(status.isError() ? PROBLEM_JSON : ContentType.JSON.toString())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, responseType);
  }
}
