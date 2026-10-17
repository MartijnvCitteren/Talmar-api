package eu.talmar.api.adapter.in.rest.generation;

import eu.talmar.api.domain.generation.VacancyDescriptionGenerationFailedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationRejectedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationUnavailableException;
import eu.talmar.api.shared.Constants;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetailsErrorsInner;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates generation-specific exceptions into the {@code ProblemDetails} representation of the
 * API contract. One exception per response, so each handler only maps; it never inspects the
 * exception to decide on a status. A failed generation answers with a fixed detail per status: the
 * exact failure is logged, never shown to the user.
 *
 * <p>Ordered at highest precedence for the same reason as {@code VacancyExceptionHandler}: the
 * catch-all in {@code GlobalExceptionHandler} must not shadow it.
 */
@Log4j2
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GenerationExceptionHandler {

  @ExceptionHandler(VacancyDescriptionGenerationNotFoundException.class)
  ResponseEntity<ProblemDetails> handleGenerationNotFound(
      VacancyDescriptionGenerationNotFoundException ex, HttpServletRequest request) {
    return respond(HttpStatus.NOT_FOUND, Constants.NOT_FOUND_TITLE, ex.getMessage(), request);
  }

  @ExceptionHandler(VacancyDescriptionGenerationRejectedException.class)
  ResponseEntity<ProblemDetails> handleGenerationRejected(
      VacancyDescriptionGenerationRejectedException ex, HttpServletRequest request) {
    log.warn(ex.getMessage());
    return respond(
        HttpStatus.UNPROCESSABLE_CONTENT,
        Constants.GENERATION_FAILED_TITLE,
        Constants.GENERATION_REJECTED_DETAIL,
        request);
  }

  @ExceptionHandler(VacancyDescriptionGenerationFailedException.class)
  ResponseEntity<ProblemDetails> handleGenerationFailed(
      VacancyDescriptionGenerationFailedException ex, HttpServletRequest request) {
    log.error(ex.getMessage());
    return respond(
        HttpStatus.INTERNAL_SERVER_ERROR,
        Constants.GENERATION_FAILED_TITLE,
        Constants.GENERATION_FAILED_DETAIL,
        request);
  }

  @ExceptionHandler(VacancyDescriptionGenerationUnavailableException.class)
  ResponseEntity<ProblemDetails> handleGenerationUnavailable(
      VacancyDescriptionGenerationUnavailableException ex, HttpServletRequest request) {
    log.error(ex.getMessage());
    return respond(
        HttpStatus.SERVICE_UNAVAILABLE,
        Constants.GENERATION_UNAVAILABLE_TITLE,
        Constants.GENERATION_UNAVAILABLE_DETAIL,
        request);
  }

  /** Same shape as a bean-validation failure: the field is named, its value is not echoed. */
  @ExceptionHandler(InvalidGenerationRequestException.class)
  ResponseEntity<ProblemDetails> handleInvalidGenerationRequest(
      InvalidGenerationRequestException ex, HttpServletRequest request) {
    var problemDetails =
        problem(
            HttpStatus.BAD_REQUEST,
            Constants.VALIDATION_FAILED_TITLE,
            Constants.VALIDATION_FAILED_DETAIL,
            request);
    problemDetails.setErrors(
        List.of(new ProblemDetailsErrorsInner(ex.getField(), ex.getMessage())));
    return respond(HttpStatus.BAD_REQUEST, problemDetails);
  }

  private static ResponseEntity<ProblemDetails> respond(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    return respond(status, problem(status, title, detail, request));
  }

  private static ProblemDetails problem(
      HttpStatus status, String title, String detail, HttpServletRequest request) {
    var problemDetails = new ProblemDetails(title, status.value());
    problemDetails.setDetail(detail);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    return problemDetails;
  }

  private static ResponseEntity<ProblemDetails> respond(
      HttpStatus status, ProblemDetails problemDetails) {
    return ResponseEntity.status(status)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }
}
