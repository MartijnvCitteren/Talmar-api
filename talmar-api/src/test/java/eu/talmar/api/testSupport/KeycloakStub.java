package eu.talmar.api.testSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A WireMock stand-in for Keycloak's realm (discovery document and signing keys), its token
 * endpoint and its Admin REST API, shared by every test that runs the real Keycloak adapter or
 * sends an access token. Tests sign their own access tokens with the realm's key ({@link
 * #accessToken}), so the resource server validates them exactly as it validates Keycloak's. One
 * server for the whole test run, for the same reason as {@link MistralStub}: cached Spring contexts
 * keep pointing at its port. Tests reset it before use.
 */
public final class KeycloakStub {
  public static final String REALM = "talmar";
  public static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
  public static final String ADMIN_PATH = "/admin/realms/" + REALM;
  public static final String SERVICE_ACCOUNT_TOKEN = "service-account-token";
  private static final String REALM_PATH = "/realms/" + REALM;
  private static final String CERTS_PATH = REALM_PATH + "/protocol/openid-connect/certs";
  private static final Clock CLOCK = Clock.systemUTC();
  private static final RSAKey REALM_KEY = generateKey("realm-key");
  private static final RSAKey OTHER_KEY = generateKey("other-key");

  public static final WireMockServer KEYCLOAK_WIREMOCK =
      new WireMockServer(wireMockConfig().dynamicPort().http2PlainDisabled(true));

  static {
    KEYCLOAK_WIREMOCK.start();
  }

  private KeycloakStub() {}

  /** The issuer the realm's tokens carry; the backend's {@code issuer-uri} points here. */
  public static String issuer() {
    return KEYCLOAK_WIREMOCK.baseUrl() + REALM_PATH;
  }

  /**
   * Serves what the resource server and the adapter fetch before their first call: the discovery
   * document, the realm's public key and the service-account token.
   */
  public static void stubRealm() {
    KEYCLOAK_WIREMOCK.stubFor(
        get(REALM_PATH + "/.well-known/openid-configuration").willReturn(json(discovery())));
    KEYCLOAK_WIREMOCK.stubFor(
        get(CERTS_PATH)
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(new JWKSet(REALM_KEY.toPublicJWK()).toString())));
    stubServiceAccountToken();
  }

  /** An access token for the account, signed by the realm and valid for five minutes. */
  public static String accessToken(String subject) {
    return sign(REALM_KEY, subject, Instant.now(CLOCK).plus(Duration.ofMinutes(5)));
  }

  /**
   * An access token that expired two minutes ago, beyond the resource server's minute of clock
   * skew.
   */
  public static String expiredAccessToken(String subject) {
    return sign(REALM_KEY, subject, Instant.now(CLOCK).minus(Duration.ofMinutes(2)));
  }

  /** An access token signed with a key the realm does not publish. */
  public static String foreignAccessToken(String subject) {
    return sign(OTHER_KEY, subject, Instant.now(CLOCK).plus(Duration.ofMinutes(5)));
  }

  /** Lets the token endpoint hand out the service-account token, valid for five minutes. */
  public static void stubServiceAccountToken() {
    KEYCLOAK_WIREMOCK.stubFor(post(TOKEN_PATH).willReturn(json(tokenResponse())));
  }

  /** A {@code 200} with a JSON body. */
  public static ResponseDefinitionBuilder json(Object body) {
    return aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(JSON_MAPPER.writeValueAsString(body));
  }

  /** Keycloak's {@code UserRepresentation} with the fields the adapter reads. */
  public static Map<String, Object> user(
      String id, String firstName, String lastName, String email, boolean emailVerified) {
    return Map.of(
        "id", id,
        "username", email,
        "firstName", firstName,
        "lastName", lastName,
        "email", email,
        "emailVerified", emailVerified,
        "enabled", true);
  }

  private static String sign(RSAKey key, String subject, Instant expiresAt) {
    var header =
        new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(key.getKeyID())
            .type(JOSEObjectType.JWT)
            .build();
    var claims =
        new JWTClaimsSet.Builder()
            .issuer(issuer())
            .subject(subject)
            .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(5))))
            .expirationTime(Date.from(expiresAt))
            .claim("azp", "talmar-web")
            .build();
    var jwt = new SignedJWT(header, claims);
    try {
      jwt.sign(new RSASSASigner(key));
    } catch (JOSEException e) {
      throw new IllegalStateException("Cannot sign a test access token", e);
    }
    return jwt.serialize();
  }

  private static RSAKey generateKey(String keyId) {
    try {
      return new RSAKeyGenerator(2048).keyID(keyId).generate();
    } catch (JOSEException e) {
      throw new IllegalStateException("Cannot generate a test signing key", e);
    }
  }

  private static Map<String, Object> discovery() {
    return Map.of(
        "issuer", issuer(),
        "jwks_uri", KEYCLOAK_WIREMOCK.baseUrl() + CERTS_PATH,
        "token_endpoint", KEYCLOAK_WIREMOCK.baseUrl() + TOKEN_PATH,
        "authorization_endpoint", issuer() + "/protocol/openid-connect/auth",
        "response_types_supported", List.of("code"),
        "subject_types_supported", List.of("public"),
        "id_token_signing_alg_values_supported", List.of("RS256"));
  }

  private static Map<String, Object> tokenResponse() {
    return Map.of(
        "access_token",
        SERVICE_ACCOUNT_TOKEN,
        "token_type",
        "Bearer",
        "expires_in",
        300,
        "scope",
        "profile email");
  }
}
