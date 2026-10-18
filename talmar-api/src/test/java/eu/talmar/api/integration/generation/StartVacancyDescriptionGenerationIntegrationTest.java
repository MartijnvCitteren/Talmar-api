package eu.talmar.api.integration.generation;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.MOST_IMPORTANT_TASKS;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.PROBLEM_JSON;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.SALES_MANAGER_VACANCY_ID;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.SEED_SCRIPT;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.TEAM;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.VALID_REQUEST;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.WHY_NICE_JOB;
import static eu.talmar.api.testSupport.MistralStub.COMPLETIONS;
import static eu.talmar.api.testSupport.MistralStub.MISTRAL_WIREMOCK;
import static eu.talmar.api.testSupport.MistralStub.draftCompletion;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionGeneration;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionGenerationStatus;
import io.restassured.http.ContentType;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Starting a description generation ({@code POST /vacancy/{id}/generate-description}), only through
 * the API. Mistral is the only thing replaced (WireMock); the request it receives is the observable
 * effect of a start.
 */
@Sql(SEED_SCRIPT)
class StartVacancyDescriptionGenerationIntegrationTest extends BaseIntegrationTest {

  @BeforeEach
  void resetMistral() {
    MISTRAL_WIREMOCK.resetAll();
    MISTRAL_WIREMOCK.stubFor(post(COMPLETIONS).willReturn(draftCompletion(true)));
  }

  @Test
  @DisplayName(
      "given an existing vacancy and valid answers, when a generation is started then 202 with a"
          + " pending generation and its poll location")
  void givenExistingVacancyAndValidAnswersWhenStartedThen202WithPendingGenerationAndLocation() {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(VALID_REQUEST)
            .when()
            .post("/vacancy/{id}/generate-description", SALES_MANAGER_VACANCY_ID)
            .then()
            .statusCode(HttpStatus.ACCEPTED.value())
            .extract();
    var started = JSON_MAPPER.readValue(response.asString(), VacancyDescriptionGeneration.class);

    assertEquals(VacancyDescriptionGenerationStatus.PENDING, started.getStatus());
    assertTrue(
        response
            .header("Location")
            .endsWith(
                "/vacancy/"
                    + SALES_MANAGER_VACANCY_ID
                    + "/generate-description/"
                    + started.getGenerationId()));
  }

  @Test
  @DisplayName(
      "given an existing vacancy, when a generation is started then Mistral is asked in the"
          + " vacancy's language with its job title and the answers")
  void givenExistingVacancyWhenStartedThenMistralAskedWithVacancyFactsAndAnswers() {
    GenerationIntegrationCommons.startGeneration(SALES_MANAGER_VACANCY_ID);

    MISTRAL_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(COMPLETIONS))
            .withRequestBody(matchingJsonPath("$.messages[0].content", containing("Dutch")))
            .withRequestBody(matchingJsonPath("$.messages[1].content", containing("Sales Manager")))
            .withRequestBody(
                matchingJsonPath("$.messages[1].content", containing(MOST_IMPORTANT_TASKS)))
            .withRequestBody(matchingJsonPath("$.messages[1].content", containing(TEAM)))
            .withRequestBody(matchingJsonPath("$.messages[1].content", containing(WHY_NICE_JOB))));
  }

  @Test
  @DisplayName("given an unknown vacancy, when a generation is started then 404 and Mistral unused")
  void givenUnknownVacancyWhenStartedThen404AndMistralUnused() {
    given()
        .contentType(ContentType.JSON)
        .body(VALID_REQUEST)
        .when()
        .post("/vacancy/{id}/generate-description", UUID.randomUUID())
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .contentType(PROBLEM_JSON);

    MISTRAL_WIREMOCK.verify(0, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidRequests")
  @DisplayName(
      "given an invalid answer, when a generation is started then 400 naming the field and"
          + " Mistral unused")
  void givenInvalidAnswerWhenStartedThen400NamingTheFieldAndMistralUnused(
      String violation, String expectedField, String requestBody) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(requestBody)
            .when()
            .post("/vacancy/{id}/generate-description", SALES_MANAGER_VACANCY_ID)
            .then()
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    var problem = JSON_MAPPER.readValue(response, ProblemDetails.class);

    assertEquals(expectedField, problem.getErrors().getFirst().getField());
    MISTRAL_WIREMOCK.verify(0, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }

  private static Stream<Arguments> invalidRequests() {
    return Stream.of(
        arguments(
            "team contains a script tag",
            "team",
            "{\"mostImportantTasks\": \"Sell\", \"team\": \"<script>x</script>\","
                + " \"whyNiceJob\": \"Fun\"}"),
        arguments(
            "whyNiceJob contains a control character",
            "whyNiceJob",
            "{\"mostImportantTasks\": \"Sell\", \"team\": \"Five\", \"whyNiceJob\":"
                + " \"Fun\\u0007\"}"),
        arguments(
            "mostImportantTasks exceeds 1000 characters",
            "mostImportantTasks",
            "{\"mostImportantTasks\": \""
                + "a".repeat(1001)
                + "\", \"team\": \"Five\", \"whyNiceJob\": \"Fun\"}"),
        arguments(
            "team is missing",
            "team",
            "{\"mostImportantTasks\": \"Sell\", \"whyNiceJob\": \"Fun\"}"));
  }
}
