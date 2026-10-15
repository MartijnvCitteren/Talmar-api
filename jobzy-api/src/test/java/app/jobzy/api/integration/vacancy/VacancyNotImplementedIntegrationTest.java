package app.jobzy.api.integration.vacancy;

import static app.jobzy.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import app.jobzy.api.integration.BaseIntegrationTest;
import app.jobzy.api.vacancy.adapter.in.web.contract.ProblemDetails;
import io.restassured.http.Method;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

/**
 * Every operation the contract declares but the controller does not implement yet must answer with
 * an explicit 501 problem response, never with an empty success.
 */
class VacancyNotImplementedIntegrationTest extends BaseIntegrationTest {

  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String MERGE_PATCH_JSON = "application/merge-patch+json";
  private static final String VACANCY = "/vacancy/" + UUID.randomUUID();

  @ParameterizedTest(name = "{0}")
  @MethodSource("notImplementedOperations")
  @DisplayName(
      "given an operation that is not built yet, when called then it responds with a 501 problem"
          + " detail naming the operation")
  void givenNotImplementedOperationWhenCalledThenResponds501ProblemDetail(
      String operationId, Method method, String path, String contentType, String body) {
    var request = given();
    if (body != null) {
      request.contentType(contentType).body(body);
    }

    var response =
        request
            .when()
            .request(method, path)
            .then()
            .statusCode(HttpStatus.NOT_IMPLEMENTED.value())
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();

    var problemDetails = JSON_MAPPER.readValue(response, ProblemDetails.class);
    assertEquals("Not implemented", problemDetails.getTitle());
    assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), problemDetails.getStatus());
    assertEquals("Operation is not implemented yet: " + operationId, problemDetails.getDetail());
  }

  private static Stream<Arguments> notImplementedOperations() {
    return Stream.of(
        arguments("getVacancy", Method.GET, VACANCY, null, null),
        arguments(
            "updateVacancy", Method.PATCH, VACANCY, MERGE_PATCH_JSON, "{\"jobTitle\": \"Chef\"}"),
        arguments("deleteVacancy", Method.DELETE, VACANCY, null, null),
        arguments("publishVacancy", Method.POST, VACANCY + "/publish", null, null),
        arguments("fillVacancy", Method.POST, VACANCY + "/fill", null, null),
        arguments("closeVacancy", Method.POST, VACANCY + "/close", null, null),
        arguments("listVacancies", Method.GET, VACANCY + "/generate-description", null, null));
  }
}
