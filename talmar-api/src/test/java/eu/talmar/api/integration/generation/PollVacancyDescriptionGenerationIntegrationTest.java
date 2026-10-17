package eu.talmar.api.integration.generation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.CHEF_VACANCY_ID;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.PROBLEM_JSON;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.SALES_MANAGER_VACANCY_ID;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.SEED_SCRIPT;
import static eu.talmar.api.integration.generation.GenerationIntegrationCommons.startGeneration;
import static eu.talmar.api.testSupport.MistralStub.COMPLETIONS;
import static eu.talmar.api.testSupport.MistralStub.JOB_DESCRIPTION;
import static eu.talmar.api.testSupport.MistralStub.MISTRAL_WIREMOCK;
import static eu.talmar.api.testSupport.MistralStub.SUMMARY;
import static eu.talmar.api.testSupport.MistralStub.TASKS;
import static eu.talmar.api.testSupport.MistralStub.completion;
import static eu.talmar.api.testSupport.MistralStub.draftCompletion;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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
 * Fetching a generated description ({@code GET /vacancy/{id}/generate-description/{generationId}}),
 * only through the API: every test starts a generation with a {@code POST} against a Mistral
 * replaced by WireMock and polls the result. {@code @Async} work has finished when the {@code POST}
 * returns, so the poll sees the final outcome.
 */
@Sql(SEED_SCRIPT)
class PollVacancyDescriptionGenerationIntegrationTest extends BaseIntegrationTest {

  @BeforeEach
  void resetMistral() {
    MISTRAL_WIREMOCK.resetAll();
  }

  @Test
  @DisplayName(
      "given Mistral drafts a description, when the generation is polled then 200 with only the"
          + " generated sections")
  void givenMistralDraftsWhenPolledThen200WithGeneratedSections() {
    mistralAnswers(draftCompletion(true));
    var generationId = startGeneration(SALES_MANAGER_VACANCY_ID);

    var polled = poll(SALES_MANAGER_VACANCY_ID, generationId);

    assertEquals(VacancyDescriptionGenerationStatus.COMPLETED, polled.getStatus());
    assertEquals(SUMMARY, polled.getDescription().getSummary());
    assertEquals(JOB_DESCRIPTION, polled.getDescription().getJobDescription());
    assertEquals(TASKS, polled.getDescription().getTasks());
    assertNull(polled.getDescription().getWhatWeOffer());
    assertNull(polled.getDescription().getAboutUs());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("failedGenerations")
  @DisplayName(
      "given a generation that failed, when polled then the problem response for that kind of"
          + " failure")
  void givenFailedGenerationWhenPolledThenProblemForThatFailure(
      String failure,
      ResponseDefinitionBuilder mistralResponse,
      HttpStatus expectedStatus,
      String expectedTitle) {
    mistralAnswers(mistralResponse);
    var generationId = startGeneration(SALES_MANAGER_VACANCY_ID);

    var problem = pollExpectingProblem(SALES_MANAGER_VACANCY_ID, generationId, expectedStatus);

    assertEquals(expectedTitle, problem.getTitle());
  }

  private static Stream<Arguments> failedGenerations() {
    var failed = "Description generation failed";
    return Stream.of(
        arguments(
            "Mistral rejects the answers as not describing a job",
            draftCompletion(false),
            HttpStatus.UNPROCESSABLE_CONTENT,
            failed),
        arguments(
            "Mistral keeps failing with a server error",
            aResponse().withStatus(503),
            HttpStatus.INTERNAL_SERVER_ERROR,
            failed),
        arguments(
            "Mistral drafts raw HTML",
            completion(
                "{\"summary\": \"<script>alert(1)</script>\", \"jobDescription\": \"J\","
                    + " \"tasks\": \"T\", \"inputRelevant\": true, \"rejectionReason\": \"\"}"),
            HttpStatus.INTERNAL_SERVER_ERROR,
            failed),
        arguments(
            "Mistral refuses the API key",
            aResponse().withStatus(401).withBody("{\"message\":\"Unauthorized\"}"),
            HttpStatus.SERVICE_UNAVAILABLE,
            "Description generation unavailable"));
  }

  @Test
  @DisplayName("given an unknown generation id, when polled then 404")
  void givenUnknownGenerationIdWhenPolledThen404() {
    pollExpectingProblem(SALES_MANAGER_VACANCY_ID, UUID.randomUUID(), HttpStatus.NOT_FOUND);
  }

  @Test
  @DisplayName("given a generation of another vacancy, when polled via this vacancy's URL then 404")
  void givenGenerationOfAnotherVacancyWhenPolledViaThisVacancyThen404() {
    mistralAnswers(draftCompletion(true));
    var generationId = startGeneration(CHEF_VACANCY_ID);

    pollExpectingProblem(SALES_MANAGER_VACANCY_ID, generationId, HttpStatus.NOT_FOUND);
  }

  private static void mistralAnswers(ResponseDefinitionBuilder response) {
    MISTRAL_WIREMOCK.stubFor(post(COMPLETIONS).willReturn(response));
  }

  private static VacancyDescriptionGeneration poll(UUID vacancyId, UUID generationId) {
    var response =
        given()
            .when()
            .get("/vacancy/{id}/generate-description/{generationId}", vacancyId, generationId)
            .then()
            .statusCode(HttpStatus.OK.value())
            .contentType(ContentType.JSON)
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, VacancyDescriptionGeneration.class);
  }

  private static ProblemDetails pollExpectingProblem(
      UUID vacancyId, UUID generationId, HttpStatus expectedStatus) {
    var response =
        given()
            .when()
            .get("/vacancy/{id}/generate-description/{generationId}", vacancyId, generationId)
            .then()
            .statusCode(expectedStatus.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, ProblemDetails.class);
  }
}
