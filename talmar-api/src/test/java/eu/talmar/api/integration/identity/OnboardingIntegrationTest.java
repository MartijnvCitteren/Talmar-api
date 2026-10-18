package eu.talmar.api.integration.identity;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static eu.talmar.api.testSupport.KeycloakStub.ADMIN_PATH;
import static eu.talmar.api.testSupport.KeycloakStub.KEYCLOAK_WIREMOCK;
import static eu.talmar.api.testSupport.KeycloakStub.accessToken;
import static eu.talmar.api.testSupport.KeycloakStub.expiredAccessToken;
import static eu.talmar.api.testSupport.KeycloakStub.foreignAccessToken;
import static eu.talmar.api.testSupport.KeycloakStub.json;
import static eu.talmar.api.testSupport.KeycloakStub.stubRealm;
import static eu.talmar.api.testSupport.KeycloakStub.user;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import eu.talmar.api.identity.adapter.in.web.contract.AccountStatus;
import eu.talmar.api.identity.adapter.in.web.contract.OnboardingResponse;
import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;

/**
 * Onboarding after the first login ({@code POST /onboarding}), only through the API. Keycloak is
 * the only thing replaced (WireMock): the tests sign access tokens with the stub realm's key, and
 * what Keycloak's Admin API receives is the observable effect of an onboarding. The seed holds the
 * onboarded company {@code acme.nl} and its founder.
 */
@Sql("/sql/identity-acme.sql")
class OnboardingIntegrationTest extends BaseIntegrationTest {
  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String VALID_REQUEST = "{\"jobRole\": \"RECRUITER\"}";
  private static final String NEW_SUBJECT = "6f1d2c3b-4a5e-4f60-8a7b-9c0d1e2f3a4b";
  private static final String NEW_USER_PATH = ADMIN_PATH + "/users/" + NEW_SUBJECT;
  private static final String ORGANIZATIONS_PATH = ADMIN_PATH + "/organizations";
  private static final String NEW_ORGANIZATION_ID = "9e8d7c6b-0000-4000-8000-00000000b0b0";
  private static final String NEW_MEMBERS_PATH =
      ORGANIZATIONS_PATH + "/" + NEW_ORGANIZATION_ID + "/members";
  private static final String NEW_DOMAIN = "globex.com";
  private static final String ACME_FOUNDER_SUBJECT = "5b9c3f0e-0000-4000-8000-00000000a003";

  @BeforeEach
  void resetKeycloak() {
    KEYCLOAK_WIREMOCK.resetAll();
    stubRealm();
  }

  @Test
  @DisplayName(
      "given the first account of a domain, when onboarding then 200 active, and Keycloak gets an"
          + " organization for the domain with the account as member")
  void givenFirstAccountOfDomainWhenOnboardingThen200ActiveWithOrganizationAndMembership() {
    stubProfile("jane@" + NEW_DOMAIN, true);
    stubOrganizationCreated();
    stubMemberAdded();

    var response = onboard(withToken(accessToken(NEW_SUBJECT)), HttpStatus.OK);

    assertEquals(AccountStatus.ACTIVE, response.getStatus());
    KEYCLOAK_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(ORGANIZATIONS_PATH))
            .withRequestBody(matchingJsonPath("$.name", equalTo(NEW_DOMAIN)))
            .withRequestBody(matchingJsonPath("$.domains[0].name", equalTo(NEW_DOMAIN))));
    KEYCLOAK_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(NEW_MEMBERS_PATH)).withRequestBody(equalTo(NEW_SUBJECT)));
  }

  @Test
  @DisplayName(
      "given a domain whose company exists, when onboarding then 200 pending approval and the"
          + " account does not join the organization")
  void givenDomainWhoseCompanyExistsWhenOnboardingThen200PendingApprovalWithoutMembership() {
    stubProfile("colleague@acme.nl", true);

    var response = onboard(withToken(accessToken(NEW_SUBJECT)), HttpStatus.OK);

    assertEquals(AccountStatus.PENDING_APPROVAL, response.getStatus());
    KEYCLOAK_WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo(ORGANIZATIONS_PATH)));
    KEYCLOAK_WIREMOCK.verify(0, postRequestedFor(urlEqualTo(NEW_MEMBERS_PATH)));
  }

  @Test
  @DisplayName(
      "given an onboarded account, when onboarding again then 409 and Keycloak is not asked for"
          + " anything")
  void givenOnboardedAccountWhenOnboardingAgainThen409AndKeycloakNotAsked() {
    var problem = onboardExpectingProblem(withToken(accessToken(ACME_FOUNDER_SUBJECT)), 409);

    assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
    KEYCLOAK_WIREMOCK.verify(
        0, getRequestedFor(urlPathEqualTo(ADMIN_PATH + "/users/" + ACME_FOUNDER_SUBJECT)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("profilesThatMayNotOnboard")
  @DisplayName(
      "given an account that may not onboard, when onboarding then 403 and no organization is"
          + " created")
  void givenAccountThatMayNotOnboardWhenOnboardingThen403AndNoOrganization(
      String reason, String email, boolean emailVerified) {
    stubProfile(email, emailVerified);

    onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 403);

    KEYCLOAK_WIREMOCK.verify(0, postRequestedFor(urlPathEqualTo(ORGANIZATIONS_PATH)));
  }

  private static Stream<Arguments> profilesThatMayNotOnboard() {
    return Stream.of(
        arguments("public mail domain", "jane@gmail.com", true),
        arguments("public mail domain under any top-level domain", "jane@hotmail.co.uk", true),
        arguments("unverified e-mail address", "jane@" + NEW_DOMAIN, false));
  }

  @Test
  @DisplayName("given an account that was refused, when onboarding again then it is refused again")
  void givenAccountThatWasRefusedWhenOnboardingAgainThenRefusedAgain() {
    stubProfile("jane@gmail.com", true);
    onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 403);

    onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 403);

    KEYCLOAK_WIREMOCK.verify(2, getRequestedFor(urlEqualTo(NEW_USER_PATH)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unauthenticatedRequests")
  @DisplayName(
      "given no valid access token, when onboarding then 401 without the reason, and Keycloak is"
          + " not asked")
  void givenNoValidAccessTokenWhenOnboardingThen401WithoutReason(
      String problem, String accessToken, String challenge) {
    var request = accessToken == null ? given() : withToken(accessToken);

    request
        .contentType(ContentType.JSON)
        .body(VALID_REQUEST)
        .when()
        .post("/onboarding")
        .then()
        .statusCode(HttpStatus.UNAUTHORIZED.value())
        .header("WWW-Authenticate", challenge)
        .body(emptyString());

    KEYCLOAK_WIREMOCK.verify(0, getRequestedFor(urlEqualTo(NEW_USER_PATH)));
  }

  private static Stream<Arguments> unauthenticatedRequests() {
    var noToken = "Bearer";
    var rejectedToken = "Bearer error=\"invalid_token\"";
    return Stream.of(
        arguments("no token", null, noToken),
        arguments("expired token", expiredAccessToken(NEW_SUBJECT), rejectedToken),
        arguments("token signed with another key", foreignAccessToken(NEW_SUBJECT), rejectedToken),
        arguments("not a token", "not-a-jwt", rejectedToken));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"jobRole\": \"CEO\"}"})
  @DisplayName("given a body that breaks the contract, when onboarding then 400")
  void givenBodyThatBreaksTheContractWhenOnboardingThen400(String body) {
    withToken(accessToken(NEW_SUBJECT))
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/onboarding")
        .then()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .contentType(PROBLEM_JSON);
  }

  @Test
  @DisplayName("given Keycloak is unavailable, when onboarding then 503")
  void givenKeycloakIsUnavailableWhenOnboardingThen503() {
    KEYCLOAK_WIREMOCK.stubFor(get(NEW_USER_PATH).willReturn(aResponse().withStatus(503)));

    var problem = onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 503);

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problem.getStatus());
  }

  @Test
  @DisplayName(
      "given creating the organization failed, when onboarding again then it finishes without"
          + " fetching the profile again")
  void givenCreatingOrganizationFailedWhenOnboardingAgainThenItFinishes() {
    stubProfile("jane@" + NEW_DOMAIN, true);
    KEYCLOAK_WIREMOCK.stubFor(
        post(ORGANIZATIONS_PATH)
            .inScenario("organization")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("recovered"));
    KEYCLOAK_WIREMOCK.stubFor(
        post(ORGANIZATIONS_PATH)
            .inScenario("organization")
            .whenScenarioStateIs("recovered")
            .willReturn(organizationCreated()));
    stubMemberAdded();
    onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 503);

    var response = onboard(withToken(accessToken(NEW_SUBJECT)), HttpStatus.OK);

    assertEquals(AccountStatus.ACTIVE, response.getStatus());
    KEYCLOAK_WIREMOCK.verify(1, getRequestedFor(urlEqualTo(NEW_USER_PATH)));
    KEYCLOAK_WIREMOCK.verify(1, postRequestedFor(urlEqualTo(NEW_MEMBERS_PATH)));
  }

  @Test
  @DisplayName(
      "given adding the member failed, when onboarding again then only the membership is retried")
  void givenAddingMemberFailedWhenOnboardingAgainThenOnlyMembershipIsRetried() {
    stubProfile("jane@" + NEW_DOMAIN, true);
    stubOrganizationCreated();
    KEYCLOAK_WIREMOCK.stubFor(
        post(NEW_MEMBERS_PATH)
            .inScenario("member")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("recovered"));
    KEYCLOAK_WIREMOCK.stubFor(
        post(NEW_MEMBERS_PATH)
            .inScenario("member")
            .whenScenarioStateIs("recovered")
            .willReturn(aResponse().withStatus(201)));
    onboardExpectingProblem(withToken(accessToken(NEW_SUBJECT)), 503);

    var response = onboard(withToken(accessToken(NEW_SUBJECT)), HttpStatus.OK);

    assertEquals(AccountStatus.ACTIVE, response.getStatus());
    KEYCLOAK_WIREMOCK.verify(1, postRequestedFor(urlEqualTo(ORGANIZATIONS_PATH)));
    KEYCLOAK_WIREMOCK.verify(2, postRequestedFor(urlEqualTo(NEW_MEMBERS_PATH)));
  }

  @Test
  @DisplayName(
      "given Keycloak already has the organization, when onboarding then the existing one is"
          + " linked")
  void givenKeycloakAlreadyHasOrganizationWhenOnboardingThenExistingOneIsLinked() {
    stubProfile("jane@" + NEW_DOMAIN, true);
    KEYCLOAK_WIREMOCK.stubFor(post(ORGANIZATIONS_PATH).willReturn(aResponse().withStatus(409)));
    KEYCLOAK_WIREMOCK.stubFor(
        get(urlPathEqualTo(ORGANIZATIONS_PATH))
            .willReturn(
                json(
                    List.of(
                        Map.of(
                            "id",
                            NEW_ORGANIZATION_ID,
                            "name",
                            NEW_DOMAIN,
                            "enabled",
                            true,
                            "domains",
                            List.of(Map.of("name", NEW_DOMAIN)))))));
    stubMemberAdded();

    var response = onboard(withToken(accessToken(NEW_SUBJECT)), HttpStatus.OK);

    assertEquals(AccountStatus.ACTIVE, response.getStatus());
    KEYCLOAK_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(NEW_MEMBERS_PATH)).withRequestBody(equalTo(NEW_SUBJECT)));
  }

  private static RequestSpecification withToken(String accessToken) {
    return given().auth().oauth2(accessToken);
  }

  private static OnboardingResponse onboard(RequestSpecification request, HttpStatus expected) {
    var body =
        request
            .contentType(ContentType.JSON)
            .body(VALID_REQUEST)
            .when()
            .post("/onboarding")
            .then()
            .statusCode(expected.value())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(body, OnboardingResponse.class);
  }

  private static ProblemDetails onboardExpectingProblem(RequestSpecification request, int status) {
    var body =
        request
            .contentType(ContentType.JSON)
            .body(VALID_REQUEST)
            .when()
            .post("/onboarding")
            .then()
            .statusCode(status)
            .contentType(PROBLEM_JSON)
            .extract()
            .asString();
    return JSON_MAPPER.readValue(body, ProblemDetails.class);
  }

  private static void stubProfile(String email, boolean emailVerified) {
    KEYCLOAK_WIREMOCK.stubFor(
        get(NEW_USER_PATH)
            .willReturn(json(user(NEW_SUBJECT, "Jane", "Doe", email, emailVerified))));
  }

  private static void stubOrganizationCreated() {
    KEYCLOAK_WIREMOCK.stubFor(post(ORGANIZATIONS_PATH).willReturn(organizationCreated()));
  }

  private static ResponseDefinitionBuilder organizationCreated() {
    return aResponse()
        .withStatus(201)
        .withHeader(
            "Location",
            KEYCLOAK_WIREMOCK.baseUrl() + ORGANIZATIONS_PATH + "/" + NEW_ORGANIZATION_ID);
  }

  private static void stubMemberAdded() {
    KEYCLOAK_WIREMOCK.stubFor(post(NEW_MEMBERS_PATH).willReturn(aResponse().withStatus(201)));
  }
}
