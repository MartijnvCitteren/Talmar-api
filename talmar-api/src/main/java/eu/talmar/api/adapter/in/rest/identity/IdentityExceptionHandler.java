package eu.talmar.api.adapter.in.rest.identity;

import eu.talmar.api.application.port.out.IdentityProviderUnavailableException;
import eu.talmar.api.domain.identity.OnboardingNotAllowedException;
import eu.talmar.api.domain.identity.UserAlreadyOnboardedException;
import eu.talmar.api.domain.identity.UserNotOnboardedException;
import eu.talmar.api.shared.Constants;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates identity exceptions into the {@code ProblemDetails} representation of the identity
 * contract. The problem model is the one generated from the vacancy contract: the identity codegen
 * maps its identical schema onto it. The details are fixed texts: the identity-provider adapter has
 * logged what went wrong, and the response does not repeat it.
 *
 * <p>Ordered at highest precedence so it is consulted before {@code GlobalExceptionHandler}'s
 * catch-all, which would otherwise turn these into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

  @ExceptionHandler(OnboardingNotAllowedException.class)
  ResponseEntity<ProblemDetails> handleOnboardingNotAllowedException(
      OnboardingNotAllowedException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.FORBIDDEN,
        Constants.ONBOARDING_NOT_ALLOWED_TITLE,
        Constants.ONBOARDING_NOT_ALLOWED_DETAIL,
        request);
  }

  @ExceptionHandler(UserAlreadyOnboardedException.class)
  ResponseEntity<ProblemDetails> handleUserAlreadyOnboardedException(
      UserAlreadyOnboardedException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT,
        Constants.ALREADY_ONBOARDED_TITLE,
        Constants.ALREADY_ONBOARDED_DETAIL,
        request);
  }

  @ExceptionHandler(UserNotOnboardedException.class)
  ResponseEntity<ProblemDetails> handleUserNotOnboardedException(
      UserNotOnboardedException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.NOT_FOUND,
        Constants.NOT_ONBOARDED_TITLE,
        Constants.NOT_ONBOARDED_DETAIL,
        request);
  }

  @ExceptionHandler(IdentityProviderUnavailableException.class)
  ResponseEntity<ProblemDetails> handleIdentityProviderUnavailableException(
      IdentityProviderUnavailableException ex, HttpServletRequest request) {
    return problem(
        HttpStatus.SERVICE_UNAVAILABLE,
        Constants.IDENTITY_PROVIDER_UNAVAILABLE_TITLE,
        Constants.IDENTITY_PROVIDER_UNAVAILABLE_DETAIL,
        request);
  }

  private static ResponseEntity<ProblemDetails> problem(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    var problemDetails = new ProblemDetails(title, status.value());
    problemDetails.setDetail(detail);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }
}
