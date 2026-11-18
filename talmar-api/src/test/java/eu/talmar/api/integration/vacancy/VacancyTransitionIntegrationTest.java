package eu.talmar.api.integration.vacancy;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyTransitionAction;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

/** Moving a vacancy to its next status ({@code POST /vacancy/{id}/transitions}). */
class VacancyTransitionIntegrationTest extends BaseIntegrationTest {

  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String TRANSITIONS = "/vacancy/{id}/transitions";
  private static final UUID VACANCY_ID = UUID.fromString("0199a000-0000-7000-8000-000000000011");

  @ParameterizedTest(name = "{0}")
  @EnumSource(VacancyTransitionAction.class)
  @DisplayName(
      "given a valid action, when a transition is requested then 501 because transitions are not"
          + " built yet")
  void givenValidActionWhenTransitionRequestedThen501(VacancyTransitionAction action) {
    var response =
        postTransition("{\"action\": \"%s\"}".formatted(action.getValue()))
            .statusCode(HttpStatus.NOT_IMPLEMENTED.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();

    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);
    assertEquals("Operation is not implemented yet: transitionVacancy", problemDetails.getDetail());
  }

  @Test
  @DisplayName("given a request without action, when a transition is requested then 400 naming it")
  void givenRequestWithoutActionWhenTransitionRequestedThen400NamingIt() {
    var response =
        postTransition("{}")
            .statusCode(HttpStatus.BAD_REQUEST.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();

    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);
    assertEquals("action", problemDetails.getErrors().getFirst().getField());
  }

  @Test
  @DisplayName("given an unknown action, when a transition is requested then 400")
  void givenUnknownActionWhenTransitionRequestedThen400() {
    postTransition("{\"action\": \"ARCHIVE\"}")
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .contentType(PROBLEM_JSON);
  }

  private static ValidatableResponse postTransition(String body) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post(TRANSITIONS, VACANCY_ID)
        .then();
  }
}
