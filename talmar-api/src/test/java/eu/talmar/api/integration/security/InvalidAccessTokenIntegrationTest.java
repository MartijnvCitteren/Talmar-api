package eu.talmar.api.integration.security;

import static eu.talmar.api.testSupport.KeycloakStub.KEYCLOAK_WIREMOCK;
import static eu.talmar.api.testSupport.KeycloakStub.expiredAccessToken;
import static eu.talmar.api.testSupport.KeycloakStub.foreignAccessToken;
import static eu.talmar.api.testSupport.KeycloakStub.stubRealm;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.talmar.api.integration.BaseIntegrationTest;
import io.restassured.http.ContentType;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

/**
 * While security is relaxed, an endpoint that does not need a token ignores one that is not valid,
 * instead of answering {@code 401}.
 */
class InvalidAccessTokenIntegrationTest extends BaseIntegrationTest {
  private static final String SUBJECT = "6f1d2c3b-4a5e-4f60-8a7b-9c0d1e2f3a4b";
  private static final String VALID_VACANCY_REQUEST =
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

  @BeforeEach
  void resetKeycloak() {
    KEYCLOAK_WIREMOCK.resetAll();
    stubRealm();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidAccessTokens")
  @DisplayName(
      "given an invalid access token, when calling an open endpoint then the token is ignored")
  void givenInvalidAccessTokenWhenCallingOpenEndpointThenTokenIsIgnored(
      String problem, String accessToken) {
    given()
        .auth()
        .oauth2(accessToken)
        .contentType(ContentType.JSON)
        .body(VALID_VACANCY_REQUEST)
        .when()
        .post("/vacancy")
        .then()
        .statusCode(HttpStatus.CREATED.value());
  }

  private static Stream<Arguments> invalidAccessTokens() {
    return Stream.of(
        arguments("expired token", expiredAccessToken(SUBJECT)),
        arguments("token signed with another key", foreignAccessToken(SUBJECT)),
        arguments("not a token", "not-a-jwt"));
  }
}
