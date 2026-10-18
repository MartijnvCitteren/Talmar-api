package eu.talmar.api.testSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.util.Map;

/**
 * A WireMock stand-in for Keycloak's token endpoint and Admin REST API, shared by every test that
 * runs the real Keycloak adapter. One server for the whole test run, for the same reason as {@link
 * MistralStub}: cached Spring contexts keep pointing at its port. Tests reset it before use.
 */
public final class KeycloakStub {
  public static final String REALM = "talmar";
  public static final String TOKEN_PATH = "/realms/" + REALM + "/protocol/openid-connect/token";
  public static final String ADMIN_PATH = "/admin/realms/" + REALM;
  public static final String SERVICE_ACCOUNT_TOKEN = "service-account-token";

  public static final WireMockServer KEYCLOAK_WIREMOCK =
      new WireMockServer(wireMockConfig().dynamicPort().http2PlainDisabled(true));

  static {
    KEYCLOAK_WIREMOCK.start();
  }

  private KeycloakStub() {}

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
