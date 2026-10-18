package eu.talmar.api.integration.identity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static eu.talmar.api.testSupport.KeycloakStub.ADMIN_PATH;
import static eu.talmar.api.testSupport.KeycloakStub.KEYCLOAK_WIREMOCK;
import static eu.talmar.api.testSupport.KeycloakStub.accessToken;
import static eu.talmar.api.testSupport.KeycloakStub.stubRealm;
import static io.restassured.RestAssured.given;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import eu.talmar.api.integration.BaseIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Changing the logged-in user's names ({@code PATCH /me}), only through the API. Keycloak is the
 * only thing replaced (WireMock); the names its Admin API receives are the observable effect. That
 * Talmar's copy changed shows in the next request: a change of one name sends the other name as the
 * copy holds it. The seed holds the onboarded founder Anna de Vries of {@code acme.nl}.
 */
@Sql("/sql/identity-acme.sql")
class ChangeNameIntegrationTest extends BaseIntegrationTest {
  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String FOUNDER_SUBJECT = "5b9c3f0e-0000-4000-8000-00000000a003";
  private static final String FOUNDER_PATH = ADMIN_PATH + "/users/" + FOUNDER_SUBJECT;
  private static final String FOUNDER_FIRST_NAME = "Anna";
  private static final String FOUNDER_LAST_NAME = "de Vries";

  @BeforeEach
  void resetKeycloak() {
    KEYCLOAK_WIREMOCK.resetAll();
    stubRealm();
    KEYCLOAK_WIREMOCK.stubFor(put(FOUNDER_PATH).willReturn(aResponse().withStatus(204)));
  }

  @Test
  @DisplayName("given both names, when changed then 204 and Keycloak gets both")
  void givenBothNamesWhenChangedThen204AndKeycloakGetsBoth() {
    var names = Map.of("firstName", "José", "lastName", "'t Hooft");

    changeName(FOUNDER_SUBJECT, names, HttpStatus.NO_CONTENT);

    verifyKeycloakGot(names.get("firstName"), names.get("lastName"));
  }

  @Test
  @DisplayName(
      "given a changed last name, when the first name is changed next then Keycloak gets the new"
          + " last name from the copy")
  void givenChangedLastNameWhenFirstNameIsChangedNextThenKeycloakGetsNewLastName() {
    var lastName = Map.of("lastName", "van den Berg");
    var firstName = Map.of("firstName", "Anne-Marie");
    changeName(FOUNDER_SUBJECT, lastName, HttpStatus.NO_CONTENT);

    changeName(FOUNDER_SUBJECT, firstName, HttpStatus.NO_CONTENT);

    verifyKeycloakGot(FOUNDER_FIRST_NAME, lastName.get("lastName"));
    verifyKeycloakGot(firstName.get("firstName"), lastName.get("lastName"));
  }

  @Test
  @DisplayName("given no names, when changed then 204 and Keycloak is not called")
  void givenNoNamesWhenChangedThen204AndKeycloakNotCalled() {
    changeName(FOUNDER_SUBJECT, Map.of(), HttpStatus.NO_CONTENT);

    KEYCLOAK_WIREMOCK.verify(0, putRequestedFor(urlPathMatching(ADMIN_PATH + "/users/.*")));
  }

  @Test
  @DisplayName("given an account that has not onboarded, when changed then 404")
  void givenAccountThatHasNotOnboardedWhenChangedThen404() {
    var subject = "6f1d2c3b-4a5e-4f60-8a7b-9c0d1e2f3a4b";

    withToken(subject)
        .body(JSON_MAPPER.writeValueAsString(Map.of("firstName", "Jane")))
        .when()
        .patch("/me")
        .then()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .contentType(PROBLEM_JSON);

    KEYCLOAK_WIREMOCK.verify(0, putRequestedFor(urlPathMatching(ADMIN_PATH + "/users/.*")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "Jane2",
        "Jane_",
        "Anne  Marie",
        " Jane",
        "JJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJJ"
      })
  @DisplayName(
      "given a name that breaks the contract, when changed then 400 and Keycloak not called")
  void givenNameThatBreaksTheContractWhenChangedThen400(String invalidName) {
    withToken(FOUNDER_SUBJECT)
        .body(JSON_MAPPER.writeValueAsString(Map.of("lastName", invalidName)))
        .when()
        .patch("/me")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .contentType(PROBLEM_JSON);

    KEYCLOAK_WIREMOCK.verify(0, putRequestedFor(urlEqualTo(FOUNDER_PATH)));
  }

  @Test
  @DisplayName("given no access token, when changed then 401")
  void givenNoAccessTokenWhenChangedThen401() {
    given()
        .contentType(ContentType.JSON)
        .body(JSON_MAPPER.writeValueAsString(Map.of("firstName", "Jane")))
        .when()
        .patch("/me")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value());
  }

  @Test
  @DisplayName(
      "given Keycloak is unavailable, when changed then 503 and the copy keeps the old name")
  void givenKeycloakIsUnavailableWhenChangedThen503AndCopyKeepsOldName() {
    KEYCLOAK_WIREMOCK.stubFor(
        put(FOUNDER_PATH)
            .inScenario("keycloak down")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("recovered"));
    KEYCLOAK_WIREMOCK.stubFor(
        put(FOUNDER_PATH)
            .inScenario("keycloak down")
            .whenScenarioStateIs("recovered")
            .willReturn(aResponse().withStatus(204)));
    var firstName = Map.of("firstName", "Anne-Marie");
    changeName(FOUNDER_SUBJECT, Map.of("lastName", "van den Berg"), HttpStatus.SERVICE_UNAVAILABLE);

    changeName(FOUNDER_SUBJECT, firstName, HttpStatus.NO_CONTENT);

    verifyKeycloakGot(firstName.get("firstName"), FOUNDER_LAST_NAME);
  }

  private static RequestSpecification withToken(String subject) {
    return given().auth().oauth2(accessToken(subject)).contentType(ContentType.JSON);
  }

  private static void changeName(String subject, Map<String, String> names, HttpStatus expected) {
    withToken(subject)
        .body(JSON_MAPPER.writeValueAsString(names))
        .when()
        .patch("/me")
        .then()
        .statusCode(expected.value());
  }

  private static void verifyKeycloakGot(String firstName, String lastName) {
    KEYCLOAK_WIREMOCK.verify(
        putRequestedFor(urlEqualTo(FOUNDER_PATH))
            .withRequestBody(
                equalToJson(
                    JSON_MAPPER.writeValueAsString(
                        Map.of("firstName", firstName, "lastName", lastName)))));
  }
}
