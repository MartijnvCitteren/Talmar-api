package eu.talmar.api.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Resolves the bearer token of a request, but treats a token that is not valid (expired, forged,
 * malformed) as no token at all, instead of answering {@code 401}. While security is relaxed, a
 * stale token left behind in a client must not lock it out of endpoints that do not need one. An
 * endpoint that does need a token still answers {@code 401}, as for a request without one.
 *
 * <p>Checking the token here decodes a valid token twice per request (here and in the
 * authentication provider). Both checks are local, with Keycloak's cached keys: microseconds, not a
 * call to Keycloak.
 *
 * <p>Only a token that is itself bad is ignored. When Keycloak's keys cannot be fetched, the
 * failure still propagates and the request fails, as before.
 */
@Log4j2
@RequiredArgsConstructor
class LenientBearerTokenResolver implements BearerTokenResolver {
  private final BearerTokenResolver headerResolver = new DefaultBearerTokenResolver();
  private final JwtDecoder jwtDecoder;

  @Override
  public @Nullable String resolve(HttpServletRequest request) {
    try {
      var token = headerResolver.resolve(request);
      if (token != null) {
        jwtDecoder.decode(token);
      }
      return token;
    } catch (BadJwtException | OAuth2AuthenticationException rejected) {
      log.warn(
          "Ignored invalid access token for {} {}: {}",
          request.getMethod(),
          request.getRequestURI(),
          rejected.getMessage());
      return null;
    }
  }
}
