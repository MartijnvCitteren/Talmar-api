package eu.talmar.api.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Answers a request that is not authenticated with {@code 401} and only what RFC 6750 requires:
 * {@code error="invalid_token"} when a token was sent, a plain {@code Bearer} when none was. Either
 * way the client has to log in again, so it needs no more. Spring's default adds an {@code
 * error_description} that says why the token was rejected (expired, signed with another key,
 * malformed) in the words of the JWT library, and a link to the resource metadata; that only helps
 * someone probing with forged tokens.
 *
 * <p>What the response leaves out goes to the log, with the method and path of the request. Never
 * the token, other headers, the query string or the client's address: those can hold personal data.
 * Levels: a missing token is {@code info} (a logged-out client, or a bot), a rejected token {@code
 * warn} (an expired session, or someone probing), anything else {@code error} (not expected to
 * reach this entry point at all).
 *
 * <p>Keycloak being unreachable never gets here: Spring Security rethrows that failure, the
 * container logs it with its stack trace and answers a bare {@code 500}.
 */
@Log4j2
class BearerChallengeEntryPoint implements AuthenticationEntryPoint {
  static final String NO_TOKEN_CHALLENGE = "Bearer";
  static final String REJECTED_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
    var method = request.getMethod();
    var path = request.getRequestURI();
    switch (exception) {
      case OAuth2AuthenticationException rejected -> {
        log.warn("Rejected access token for {} {}: {}", method, path, rejected.getMessage());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, REJECTED_TOKEN_CHALLENGE);
      }
      case InsufficientAuthenticationException missing -> {
        log.info("No access token for {} {}", method, path);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, NO_TOKEN_CHALLENGE);
      }
      default -> {
        log.error("Authentication failed for {} {}", method, path, exception);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, NO_TOKEN_CHALLENGE);
      }
    }
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
  }
}
