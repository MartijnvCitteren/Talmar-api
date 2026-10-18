package eu.talmar.api.adapter.out.keycloak;

import eu.talmar.api.application.port.out.IdentityProfile;
import eu.talmar.api.application.port.out.IdentityProvider;
import eu.talmar.api.application.port.out.IdentityProviderUnavailableException;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * {@link IdentityProvider} on Keycloak's Admin REST API, called as the {@code talmar-backend}
 * service account (ADR 0006, section 6). The {@link RestClient} adds the service-account token; see
 * {@link KeycloakAdminClientConfig}.
 *
 * <p>Failures are split by whether a later retry can help, which only changes how they are logged:
 * a timeout, connection error or {@code 5xx} is temporary ({@code warn}); any other unexpected
 * answer is our bug or a misconfiguration ({@code error}). Both become an {@link
 * IdentityProviderUnavailableException}. Nothing here logs names, e-mail addresses or response
 * bodies.
 */
@Log4j2
@RequiredArgsConstructor
class KeycloakIdentityProvider implements IdentityProvider {
  private static final ParameterizedTypeReference<List<KeycloakOrganization>> ORGANIZATIONS =
      new ParameterizedTypeReference<>() {};

  private final RestClient restClient;

  @Override
  public IdentityProfile getProfile(String subject) {
    return callKeycloak("get profile", () -> toProfile(getUser(subject)));
  }

  /**
   * Sends only the two names. Keycloak 26.8 leaves every field that is missing from a {@code PUT}
   * of a user as it is (checked against a running Keycloak); the realm's user profile validates the
   * names again.
   */
  @Override
  public void changeName(String subject, String firstName, String lastName) {
    callKeycloak(
        "change name",
        () ->
            restClient
                .put()
                .uri("/users/{id}", subject)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new KeycloakNameChange(firstName, lastName))
                .retrieve()
                .toBodilessEntity());
  }

  /**
   * Creates the organization first, so the usual case is one call. Keycloak answers {@code 409}
   * when an earlier, interrupted attempt already created it; the organization is then looked up by
   * its exact domain, which Keycloak keeps unique across organizations.
   */
  @Override
  public String findOrCreateOrganization(String emailDomain, UUID companyId) {
    return callKeycloak(
        "find or create organization",
        () -> {
          var created = createOrganization(emailDomain, companyId);
          if (created.getStatusCode().isSameCodeAs(HttpStatus.CONFLICT)) {
            return findOrganizationId(emailDomain);
          }
          return idFrom(created.getHeaders().getLocation());
        });
  }

  /** Keycloak answers {@code 409} for an account that is a member already. */
  @Override
  public void addOrganizationMember(String organizationId, String subject) {
    callKeycloak(
        "add organization member",
        () ->
            restClient
                .post()
                .uri("/organizations/{id}/members", organizationId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(subject)
                .retrieve()
                .onStatus(
                    status -> status.isSameCodeAs(HttpStatus.CONFLICT), (request, response) -> {})
                .toBodilessEntity());
  }

  private KeycloakUser getUser(String subject) {
    return restClient.get().uri("/users/{id}", subject).retrieve().body(KeycloakUser.class);
  }

  private static IdentityProfile toProfile(KeycloakUser user) {
    KeycloakProfileValidator.validate(user);
    return new IdentityProfile(
        user.firstName(),
        user.lastName(),
        new EmailAddress(user.email()),
        Boolean.TRUE.equals(user.emailVerified()));
  }

  private ResponseEntity<Void> createOrganization(String emailDomain, UUID companyId) {
    return restClient
        .post()
        .uri("/organizations")
        .contentType(MediaType.APPLICATION_JSON)
        .body(KeycloakOrganization.of(emailDomain, companyId.toString()))
        .retrieve()
        .onStatus(status -> status.isSameCodeAs(HttpStatus.CONFLICT), (request, response) -> {})
        .toBodilessEntity();
  }

  private String findOrganizationId(String emailDomain) {
    var organizations =
        restClient
            .get()
            .uri(
                uri ->
                    uri.path("/organizations")
                        .queryParam("search", emailDomain)
                        .queryParam("exact", true)
                        .build())
            .retrieve()
            .body(ORGANIZATIONS);
    return Objects.requireNonNull(organizations, "Keycloak returned no organization list").stream()
        .filter(organization -> organization.hasDomain(emailDomain))
        .findFirst()
        .map(KeycloakOrganization::id)
        .orElseThrow(
            () ->
                new IllegalStateException("Keycloak reported a conflict but has no organization"));
  }

  /** Keycloak returns the new organization's URL; its last path segment is the id. */
  private static String idFrom(URI location) {
    if (location == null) {
      throw new IllegalStateException("Keycloak created an organization without a Location");
    }
    var path = location.getPath();
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private <T> T callKeycloak(String operation, Supplier<T> call) {
    try {
      return call.get();
    } catch (RuntimeException e) {
      if (isTemporary(e)) {
        log.warn("Keycloak {} failed, a retry may help: {}", operation, describe(e));
      } else {
        log.error("Keycloak {} failed: {}", operation, describe(e));
      }
      throw new IdentityProviderUnavailableException(operation);
    }
  }

  /**
   * A timeout or connection error surfaces as an I/O exception somewhere in the cause chain, also
   * when it happened while fetching the service-account token.
   */
  private static boolean isTemporary(Throwable e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpServerErrorException || cause instanceof IOException) {
        return true;
      }
    }
    return false;
  }

  /** The status for an HTTP error, without the body: Keycloak may echo the request in it. */
  private static String describe(RuntimeException e) {
    if (e instanceof RestClientResponseException responseException) {
      return "HTTP " + responseException.getStatusCode().value();
    }
    return e.toString();
  }
}
