package eu.talmar.api.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class BearerChallengeEntryPointTest {
  private static final String METHOD = "POST";
  private static final String PATH = "/api/v1/onboarding";
  private static final String REJECTION_REASON =
      "An error occurred while attempting to decode the Jwt: Jwt expired at 2026-10-18T18:49:55Z";

  private final BearerChallengeEntryPoint entryPoint = new BearerChallengeEntryPoint();
  private final Logger logger = (Logger) LoggerFactory.getLogger(BearerChallengeEntryPoint.class);
  private final ListAppender<ILoggingEvent> logged = new ListAppender<>();
  private final MockHttpServletResponse response = new MockHttpServletResponse();

  @BeforeEach
  void captureLog() {
    logged.start();
    logger.addAppender(logged);
  }

  @AfterEach
  void releaseLog() {
    logger.detachAppender(logged);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("failures")
  @DisplayName(
      "given an unauthenticated request, when commencing then 401 with only the challenge, and"
          + " the reason is logged with method and path")
  void givenUnauthenticatedRequestWhenCommencingThen401WithChallengeAndReasonLogged(
      String failure,
      AuthenticationException exception,
      String challenge,
      Level level,
      String message) {
    entryPoint.commence(new MockHttpServletRequest(METHOD, PATH), response, exception);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    assertEquals(challenge, response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    assertEquals(0, response.getContentAsByteArray().length);
    var event = logged.list.getFirst();
    assertEquals(level, event.getLevel());
    assertEquals(message, event.getFormattedMessage());
  }

  private static Stream<Arguments> failures() {
    return Stream.of(
        arguments(
            "rejected token",
            new InvalidBearerTokenException(REJECTION_REASON),
            BearerChallengeEntryPoint.REJECTED_TOKEN_CHALLENGE,
            Level.WARN,
            "Rejected access token for " + METHOD + " " + PATH + ": " + REJECTION_REASON),
        arguments(
            "no token",
            new InsufficientAuthenticationException("Full authentication is required"),
            BearerChallengeEntryPoint.NO_TOKEN_CHALLENGE,
            Level.INFO,
            "No access token for " + METHOD + " " + PATH));
  }

  @Test
  @DisplayName(
      "given an unexpected authentication failure, when commencing then 401 without a reason, and"
          + " the failure is logged as error with its stack trace")
  void givenUnexpectedFailureWhenCommencingThen401AndFailureLoggedAsError() {
    var failure = new AuthenticationServiceException("Keycloak's signing keys are unavailable");

    entryPoint.commence(new MockHttpServletRequest(METHOD, PATH), response, failure);

    assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
    assertEquals(
        BearerChallengeEntryPoint.NO_TOKEN_CHALLENGE,
        response.getHeader(HttpHeaders.WWW_AUTHENTICATE));
    var event = logged.list.getFirst();
    assertEquals(Level.ERROR, event.getLevel());
    assertEquals("Authentication failed for " + METHOD + " " + PATH, event.getFormattedMessage());
    assertSame(failure, ((ThrowableProxy) event.getThrowableProxy()).getThrowable());
  }

  @Test
  @DisplayName("given a request with a query string, when commencing then the query is not logged")
  void givenRequestWithQueryStringWhenCommencingThenQueryNotLogged() {
    var request = new MockHttpServletRequest(METHOD, PATH);
    request.setQueryString("email=jane@acme.nl");

    entryPoint.commence(
        request, response, new InsufficientAuthenticationException("Full authentication"));

    assertEquals(
        "No access token for " + METHOD + " " + PATH, logged.list.getFirst().getFormattedMessage());
  }
}
