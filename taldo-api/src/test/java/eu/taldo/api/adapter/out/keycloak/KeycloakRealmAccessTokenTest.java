package eu.taldo.api.adapter.out.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guards what the realm puts in the access tokens of the frontend's client (ADR 0006, revision
 * 2026-11-21, points 2 and 3). The client uses Keycloak's lightweight access token, which only
 * carries claims from mappers marked as lightweight: the subject and the API's audience. Names and
 * the e-mail address stay out. Without its own mapper the lightweight token has no {@code sub} at
 * all, and without the audience the backend rejects every token.
 */
class KeycloakRealmAccessTokenTest {

  private static final Path REALM_FILE = Path.of("../docker/keycloak/taldo-realm.json");
  private static final String FRONTEND_CLIENT = "taldo-web";
  private static final String AUDIENCES_PROPERTY =
      "spring.security.oauth2.resourceserver.jwt.audiences";
  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Test
  @DisplayName(
      "given the frontend client, when its access token is configured then it is a lightweight"
          + " token")
  void givenTheFrontendClientWhenItsAccessTokenIsConfiguredThenItIsALightweightToken()
      throws IOException {
    var client = frontendClient();

    assertEquals(
        "true",
        client.path("attributes").path("client.use.lightweight.access.token.enabled").asString());
  }

  @Test
  @DisplayName(
      "given the frontend client, when its access token is configured then the subject is in the"
          + " lightweight token")
  void givenTheFrontendClientWhenItsAccessTokenIsConfiguredThenTheSubjectIsInTheLightweightToken()
      throws IOException {
    var subjectMapper = lightweightAccessTokenMapper(frontendClient(), "oidc-sub-mapper");

    assertEquals("true", subjectMapper.path("config").path("access.token.claim").asString());
  }

  @Test
  @DisplayName(
      "given the frontend client, when its access token is configured then its audience is the one"
          + " the backend accepts")
  void
      givenTheFrontendClientWhenItsAccessTokenIsConfiguredThenItsAudienceIsTheOneTheBackendAccepts()
          throws IOException {
    var audienceMapper = lightweightAccessTokenMapper(frontendClient(), "oidc-audience-mapper");

    assertEquals(
        acceptedAudience(),
        audienceMapper.path("config").path("included.custom.audience").asString());
  }

  private static JsonNode frontendClient() throws IOException {
    var realm = JSON_MAPPER.readTree(Files.readString(REALM_FILE));
    return StreamSupport.stream(realm.path("clients").spliterator(), false)
        .filter(client -> FRONTEND_CLIENT.equals(client.path("clientId").asString()))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("The realm has no client " + FRONTEND_CLIENT));
  }

  /** The client's mapper of the given type, which must put its claim in the lightweight token. */
  private static JsonNode lightweightAccessTokenMapper(JsonNode client, String mapperType) {
    var mapper =
        StreamSupport.stream(client.path("protocolMappers").spliterator(), false)
            .filter(candidate -> mapperType.equals(candidate.path("protocolMapper").asString()))
            .findFirst()
            .orElseThrow(
                () -> new IllegalStateException(FRONTEND_CLIENT + " has no " + mapperType));
    assertEquals("true", mapper.path("config").path("lightweight.claim").asString());
    return mapper;
  }

  private static String acceptedAudience() {
    var yaml = new YamlPropertiesFactoryBean();
    yaml.setResources(new ClassPathResource("application.yml"));
    return Objects.requireNonNull(yaml.getObject()).getProperty(AUDIENCES_PROPERTY);
  }
}
