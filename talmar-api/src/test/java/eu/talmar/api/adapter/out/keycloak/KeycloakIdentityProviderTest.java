package eu.talmar.api.adapter.out.keycloak;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static eu.talmar.api.testSupport.KeycloakStub.ADMIN_PATH;
import static eu.talmar.api.testSupport.KeycloakStub.KEYCLOAK_WIREMOCK;
import static eu.talmar.api.testSupport.KeycloakStub.SERVICE_ACCOUNT_TOKEN;
import static eu.talmar.api.testSupport.KeycloakStub.TOKEN_PATH;
import static eu.talmar.api.testSupport.KeycloakStub.json;
import static eu.talmar.api.testSupport.KeycloakStub.stubServiceAccountToken;
import static eu.talmar.api.testSupport.KeycloakStub.user;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import eu.talmar.api.application.port.out.IdentityProviderUnavailableException;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the Keycloak adapter with the application's real client configuration (service-account
 * token, timeouts) against WireMock with the response shapes of Keycloak 26.8's Admin REST API, as
 * checked against a running Keycloak in ADR 0006. Covers what only the adapter decides: the
 * requests it sends, how it treats a conflict, which profiles it accepts and how failures surface.
 * Only the read timeout is shortened; never calls Keycloak.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class KeycloakIdentityProviderTest {
  private static final String SUBJECT = "6f1d2c3b-4a5e-4f60-8a7b-9c0d1e2f3a4b";
  private static final String USER_PATH = ADMIN_PATH + "/users/" + SUBJECT;
  private static final String ORGANIZATIONS_PATH = ADMIN_PATH + "/organizations";
  private static final String ORGANIZATION_ID = "2c2786d5-36d6-45e4-98f4-905d026f66a9";
  private static final String MEMBERS_PATH =
      ORGANIZATIONS_PATH + "/" + ORGANIZATION_ID + "/members";
  private static final String EMAIL_DOMAIN = "acme.nl";
  private static final UUID COMPANY_ID = UUID.fromString("0199c9a0-0000-7000-8000-000000000002");

  @DynamicPropertySource
  static void keycloakProperties(DynamicPropertyRegistry registry) {
    registry.add("talmar.keycloak.base-url", KEYCLOAK_WIREMOCK::baseUrl);
    registry.add("spring.http.clients.read-timeout", () -> "500ms");
  }

  @Autowired private KeycloakIdentityProvider identityProvider;
  @Autowired private OAuth2AuthorizedClientService serviceAccountTokens;

  /** Drops the token an earlier test cached, so each test sees when a token is fetched. */
  @BeforeEach
  void resetKeycloak() {
    serviceAccountTokens.removeAuthorizedClient(
        KeycloakAdminClientConfig.REGISTRATION_ID, KeycloakAdminClientConfig.REGISTRATION_ID);
    KEYCLOAK_WIREMOCK.resetAll();
    stubServiceAccountToken();
  }

  @Test
  @DisplayName(
      "given a verified account, when get profile then its names and lower-cased e-mail address"
          + " come back")
  void givenVerifiedAccountWhenGetProfileThenNamesAndEmailComeBack() {
    var keycloakUser = user(SUBJECT, "José", "'t Hooft", "Jose.tHooft@Acme.nl", true);
    KEYCLOAK_WIREMOCK.stubFor(get(USER_PATH).willReturn(json(keycloakUser)));

    var profile = identityProvider.getProfile(SUBJECT);

    assertEquals(keycloakUser.get("firstName"), profile.firstName());
    assertEquals(keycloakUser.get("lastName"), profile.lastName());
    assertEquals(new EmailAddress((String) keycloakUser.get("email")), profile.email());
  }

  @Test
  @DisplayName("given an unverified account, when get profile then it is reported as unverified")
  void givenUnverifiedAccountWhenGetProfileThenReportedAsUnverified() {
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH).willReturn(json(user(SUBJECT, "Jane", "Doe", "jane@acme.nl", false))));

    var profile = identityProvider.getProfile(SUBJECT);

    assertFalse(profile.emailVerified());
  }

  @Test
  @DisplayName(
      "given two calls, when the service-account token is still valid then it is fetched once with"
          + " the client credentials grant and sent on both calls")
  void givenTwoCallsWhenTokenStillValidThenFetchedOnceAndSentOnBoth() {
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH).willReturn(json(user(SUBJECT, "Jane", "Doe", "jane@acme.nl", true))));

    identityProvider.getProfile(SUBJECT);
    identityProvider.getProfile(SUBJECT);

    KEYCLOAK_WIREMOCK.verify(
        1,
        postRequestedFor(urlEqualTo(TOKEN_PATH))
            .withRequestBody(equalTo("grant_type=client_credentials")));
    KEYCLOAK_WIREMOCK.verify(
        2,
        getRequestedFor(urlEqualTo(USER_PATH))
            .withHeader("Authorization", equalTo("Bearer " + SERVICE_ACCOUNT_TOKEN)));
  }

  @Test
  @DisplayName(
      "given Keycloak rejects the service-account token, when called again then a new token is"
          + " fetched")
  void givenKeycloakRejectsTokenWhenCalledAgainThenNewTokenIsFetched() {
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH)
            .inScenario("revoked token")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader("WWW-Authenticate", "Bearer error=\"invalid_token\""))
            .willSetStateTo("token dropped"));
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH)
            .inScenario("revoked token")
            .whenScenarioStateIs("token dropped")
            .willReturn(json(user(SUBJECT, "Jane", "Doe", "jane@acme.nl", true))));

    assertThrows(
        IdentityProviderUnavailableException.class, () -> identityProvider.getProfile(SUBJECT));
    identityProvider.getProfile(SUBJECT);

    KEYCLOAK_WIREMOCK.verify(2, postRequestedFor(urlEqualTo(TOKEN_PATH)));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Łukasz",
        "Żółć-李",
        "O’Brien",
        "St. John",
        "Rene\u0301e",
        "Anne-Marie van den Berg"
      })
  @DisplayName("given a name the realm accepts, when get profile then it is accepted as it is")
  void givenNameTheRealmAcceptsWhenGetProfileThenAcceptedAsItIs(String lastName) {
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH).willReturn(json(user(SUBJECT, "Jane", lastName, "jane@acme.nl", true))));

    var profile = identityProvider.getProfile(SUBJECT);

    assertEquals(lastName, profile.lastName());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidProfiles")
  @DisplayName(
      "given a profile that breaks Talmar's column rules, when get profile then the provider is"
          + " unavailable")
  void givenProfileThatBreaksColumnRulesWhenGetProfileThenUnavailable(
      String problem, Map<String, Object> keycloakUser) {
    KEYCLOAK_WIREMOCK.stubFor(get(USER_PATH).willReturn(json(keycloakUser)));

    assertThrows(
        IdentityProviderUnavailableException.class, () -> identityProvider.getProfile(SUBJECT));
  }

  private static Stream<Arguments> invalidProfiles() {
    return Stream.of(
        arguments("digit in first name", user(SUBJECT, "Jane2", "Doe", "jane@acme.nl", true)),
        arguments("blank last name", user(SUBJECT, "Jane", " ", "jane@acme.nl", true)),
        arguments(
            "double space in last name", user(SUBJECT, "Jane", "van  Dijk", "jane@acme.nl", true)),
        arguments("symbol in last name", user(SUBJECT, "Jane", "Doe_", "jane@acme.nl", true)),
        arguments(
            "first name of 36 characters",
            user(SUBJECT, "J".repeat(36), "Doe", "jane@acme.nl", true)),
        arguments(
            "e-mail address of 76 characters",
            user(SUBJECT, "Jane", "Doe", "j".repeat(68) + "@acme.nl", true)),
        arguments(
            "e-mail domain without a dot", user(SUBJECT, "Jane", "Doe", "jane@localhost", true)),
        arguments("two @ signs", user(SUBJECT, "Jane", "Doe", "jane@doe@acme.nl", true)),
        arguments("empty domain label", user(SUBJECT, "Jane", "Doe", "jane@acme..nl", true)),
        arguments(
            "space in e-mail address", user(SUBJECT, "Jane", "Doe", "jane doe@acme.nl", true)),
        arguments("no last name", Map.of("id", SUBJECT, "firstName", "Jane", "email", "j@a.nl")));
  }

  @Test
  @DisplayName(
      "given a new company, when find or create organization then it is created with the domain as"
          + " name and domain and the company id as alias, and its id is returned")
  void givenNewCompanyWhenFindOrCreateOrganizationThenCreatedAndIdReturned() {
    KEYCLOAK_WIREMOCK.stubFor(
        post(ORGANIZATIONS_PATH)
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader(
                        "Location",
                        KEYCLOAK_WIREMOCK.baseUrl() + ORGANIZATIONS_PATH + "/" + ORGANIZATION_ID)));

    var organizationId = identityProvider.findOrCreateOrganization(EMAIL_DOMAIN, COMPANY_ID);

    assertEquals(ORGANIZATION_ID, organizationId);
    KEYCLOAK_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(ORGANIZATIONS_PATH))
            .withRequestBody(matchingJsonPath("$.name", equalTo(EMAIL_DOMAIN)))
            .withRequestBody(matchingJsonPath("$.alias", equalTo(COMPANY_ID.toString())))
            .withRequestBody(matchingJsonPath("$.enabled", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.domains[0].name", equalTo(EMAIL_DOMAIN))));
  }

  @Test
  @DisplayName(
      "given the organization exists already, when find or create organization then the one with"
          + " exactly that domain is returned")
  void givenOrganizationExistsWhenFindOrCreateOrganizationThenTheOneWithThatDomainIsReturned() {
    KEYCLOAK_WIREMOCK.stubFor(post(ORGANIZATIONS_PATH).willReturn(aResponse().withStatus(409)));
    KEYCLOAK_WIREMOCK.stubFor(
        get(urlPathEqualTo(ORGANIZATIONS_PATH))
            .willReturn(
                json(
                    List.of(
                        organization("other-id", "sales." + EMAIL_DOMAIN),
                        organization(ORGANIZATION_ID, EMAIL_DOMAIN)))));

    var organizationId = identityProvider.findOrCreateOrganization(EMAIL_DOMAIN, COMPANY_ID);

    assertEquals(ORGANIZATION_ID, organizationId);
    KEYCLOAK_WIREMOCK.verify(
        getRequestedFor(urlPathEqualTo(ORGANIZATIONS_PATH))
            .withQueryParam("search", equalTo(EMAIL_DOMAIN))
            .withQueryParam("exact", equalTo("true")));
  }

  @Test
  @DisplayName(
      "given a conflict but no organization with that domain, when find or create organization"
          + " then the provider is unavailable")
  void givenConflictButNoOrganizationWithThatDomainWhenFindOrCreateThenUnavailable() {
    KEYCLOAK_WIREMOCK.stubFor(post(ORGANIZATIONS_PATH).willReturn(aResponse().withStatus(409)));
    KEYCLOAK_WIREMOCK.stubFor(get(urlPathEqualTo(ORGANIZATIONS_PATH)).willReturn(json(List.of())));

    assertThrows(
        IdentityProviderUnavailableException.class,
        () -> identityProvider.findOrCreateOrganization(EMAIL_DOMAIN, COMPANY_ID));
  }

  @ParameterizedTest
  @ValueSource(ints = {201, 409})
  @DisplayName(
      "given a new or existing membership, when add organization member then it succeeds and the"
          + " subject is the body")
  void givenNewOrExistingMembershipWhenAddOrganizationMemberThenSucceeds(int status) {
    KEYCLOAK_WIREMOCK.stubFor(post(MEMBERS_PATH).willReturn(aResponse().withStatus(status)));

    assertDoesNotThrow(() -> identityProvider.addOrganizationMember(ORGANIZATION_ID, SUBJECT));

    KEYCLOAK_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(MEMBERS_PATH))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalTo(SUBJECT)));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 403, 404, 500, 503})
  @DisplayName(
      "given Keycloak answers with an error, when get profile then the provider is unavailable")
  void givenKeycloakAnswersWithErrorWhenGetProfileThenUnavailable(int status) {
    KEYCLOAK_WIREMOCK.stubFor(get(USER_PATH).willReturn(aResponse().withStatus(status)));

    assertThrows(
        IdentityProviderUnavailableException.class, () -> identityProvider.getProfile(SUBJECT));
  }

  @Test
  @DisplayName(
      "given Keycloak does not answer in time, when get profile then the provider is unavailable")
  void givenKeycloakDoesNotAnswerInTimeWhenGetProfileThenUnavailable() {
    KEYCLOAK_WIREMOCK.stubFor(
        get(USER_PATH)
            .willReturn(
                json(user(SUBJECT, "Jane", "Doe", "jane@acme.nl", true)).withFixedDelay(1000)));

    assertThrows(
        IdentityProviderUnavailableException.class, () -> identityProvider.getProfile(SUBJECT));
  }

  @Test
  @DisplayName(
      "given the token endpoint rejects the client secret, when get profile then the provider is"
          + " unavailable")
  void givenTokenEndpointRejectsClientSecretWhenGetProfileThenUnavailable() {
    KEYCLOAK_WIREMOCK.stubFor(
        post(TOKEN_PATH)
            .willReturn(
                aResponse()
                    .withStatus(401)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\":\"unauthorized_client\"}")));

    assertThrows(
        IdentityProviderUnavailableException.class, () -> identityProvider.getProfile(SUBJECT));
  }

  private static Map<String, Object> organization(String id, String domain) {
    return Map.of(
        "id",
        id,
        "name",
        domain,
        "alias",
        UUID.randomUUID().toString(),
        "enabled",
        true,
        "domains",
        List.of(Map.of("name", domain, "verified", false)));
  }
}
