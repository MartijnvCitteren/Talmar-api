package eu.talmar.api.adapter.in.rest.vacancy;

import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
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
 * Translates vacancy-specific exceptions into the {@code ProblemDetails} representation of the API
 * contract.
 *
 * <p>Ordered at highest precedence so it is consulted before {@code GlobalExceptionHandler}'s
 * catch-all {@code Exception} handler: Spring resolves exceptions per advice bean, so without an
 * explicit order the catch-all could shadow these more specific handlers.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VacancyExceptionHandler {

  /** Turns a lookup of a vacancy that does not exist into an RFC 9457 404 problem response. */
  @ExceptionHandler(VacancyNotFoundException.class)
  ResponseEntity<ProblemDetails> handleVacancyNotFoundException(
      VacancyNotFoundException ex, HttpServletRequest request) {
    var problemDetails =
        new ProblemDetails(Constants.NOT_FOUND_TITLE, HttpStatus.NOT_FOUND.value());
    problemDetails.setDetail(ex.getMessage());
    problemDetails.setInstance(URI.create(request.getRequestURI()));

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }

  /**
   * Turns a content-safety violation caught by the adapter-level validator into an RFC 9457 400
   * problem response, naming the offending field the same way bean-validation failures do.
   */
  @ExceptionHandler(InvalidVacancyDescriptionRequestException.class)
  ResponseEntity<ProblemDetails> handleInvalidVacancyDescriptionRequestException(
      InvalidVacancyDescriptionRequestException ex, HttpServletRequest request) {
    var problemDetails =
        new ProblemDetails(Constants.VALIDATION_FAILED_TITLE, HttpStatus.BAD_REQUEST.value());
    problemDetails.setDetail(Constants.VALIDATION_FAILED_DETAIL);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    problemDetails.setErrors(
        List.of(new ProblemDetailsErrorsInner(ex.getField(), ex.getMessage())));

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }
}
