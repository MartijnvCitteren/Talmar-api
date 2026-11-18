package eu.taldo.api.adapter.out.keycloak;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Where the backend reaches Keycloak ({@code taldo.keycloak.*}). The service-account credentials
 * are the {@code keycloak-admin} client registration under {@code spring.security.oauth2.client}.
 *
 * @param baseUrl Keycloak's base URL as the backend reaches it, e.g. {@code http://localhost:8080}
 * @param realm the realm that holds Taldo's accounts
 */
@ConfigurationProperties("taldo.keycloak")
record KeycloakProperties(URI baseUrl, String realm) {

  /** The root of the realm's Admin REST API. */
  String adminApiUrl() {
    return baseUrl + "/admin/realms/" + realm;
  }
}
