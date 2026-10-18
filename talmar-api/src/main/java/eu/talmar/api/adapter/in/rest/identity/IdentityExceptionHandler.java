package eu.talmar.api.adapter.in.rest.identity;

import eu.talmar.api.domain.identity.InvalidVerificationTokenException;
import eu.talmar.api.shared.Constants;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetailsErrorsInner;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates identity exceptions into the {@code ProblemDetails} representation of the identity
 * contract. Each becomes a 400 naming the field the user has to fix, in the same shape as
 * bean-validation failures, so the frontend handles both alike. The problem model is the one
 * generated from the vacancy contract: the identity codegen maps its identical schema onto it.
 *
 * <p>Ordered at highest precedence so it is consulted before {@code GlobalExceptionHandler}'s
 * catch-all, which would otherwise turn these into a 500.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

  @ExceptionHandler(PublicEmailDomainException.class)
  ResponseEntity<ProblemDetails> handlePublicEmailDomainException(
      PublicEmailDomainException ex, HttpServletRequest request) {
    return badRequest("email", ex.getMessage(), request);
  }

  @ExceptionHandler(InvalidVerificationTokenException.class)
  ResponseEntity<ProblemDetails> handleInvalidVerificationTokenException(
      InvalidVerificationTokenException ex, HttpServletRequest request) {
    return badRequest("token", ex.getMessage(), request);
  }

  private ResponseEntity<ProblemDetails> badRequest(
      String field, String message, HttpServletRequest request) {
    var problemDetails =
        new ProblemDetails(Constants.VALIDATION_FAILED_TITLE, HttpStatus.BAD_REQUEST.value());
    problemDetails.setDetail(Constants.VALIDATION_FAILED_DETAIL);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    problemDetails.setErrors(List.of(new ProblemDetailsErrorsInner(field, message)));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }
}
