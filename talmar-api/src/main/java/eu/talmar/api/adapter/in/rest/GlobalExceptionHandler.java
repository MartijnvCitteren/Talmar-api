package eu.talmar.api.adapter.in.rest;

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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions from every REST controller into the {@code ProblemDetails} representation
 * of the API contract. Aggregate-specific exceptions are handled by their own advice classes (e.g.
 * {@code VacancyExceptionHandler}); this class only covers cross-cutting concerns that apply to
 * every controller.
 *
 * <p>Ordered at lowest precedence so its catch-all {@code Exception} handler is only consulted
 * after every aggregate-specific advice has had a chance to handle the exception more precisely:
 * Spring picks the best-matching handler within the first applicable advice bean it visits, not the
 * best match across all advice beans, so an unordered catch-all here would shadow more specific
 * handlers declared in other advice classes.
 */
@Log4j2
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

  /**
   * Turns bean-validation failures on a request body into an RFC 9457 problem response. Every
   * violation is reported individually under {@code errors}, naming the offending field so the
   * client can point the user at the exact input that needs fixing. The rejected value itself is
   * deliberately not echoed back: this advice applies to every controller, so it would eventually
   * reflect (and log) personal data from candidate-facing payloads.
   *
   * <p>Returns a {@link ResponseEntity} rather than the body plus {@code @ResponseStatus}, so that
   * the {@code application/problem+json} content type the contract declares for error responses is
   * set explicitly instead of being left to content negotiation.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ProblemDetails> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException ex, HttpServletRequest request) {

    List<ProblemDetailsErrorsInner> errors =
        ex.getBindingResult().getAllErrors().stream().map(this::toError).toList();

    var problemDetails =
        new ProblemDetails(Constants.VALIDATION_FAILED_TITLE, HttpStatus.BAD_REQUEST.value());
    problemDetails.setDetail(Constants.VALIDATION_FAILED_DETAIL);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    problemDetails.setErrors(errors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }

  private ProblemDetailsErrorsInner toError(ObjectError error) {
    if (error instanceof FieldError fieldError) {
      return new ProblemDetailsErrorsInner(fieldError.getField(), fieldError.getDefaultMessage());
    }
    return new ProblemDetailsErrorsInner(error.getObjectName(), error.getDefaultMessage());
  }

  /**
   * Turns a call to a contract operation that is not built yet into an RFC 9457 501 problem
   * response, naming the operation, so clients can tell "not built yet" apart from a real failure.
   */
  @ExceptionHandler(EndpointNotImplementedException.class)
  ResponseEntity<ProblemDetails> handleEndpointNotImplementedException(
      EndpointNotImplementedException ex, HttpServletRequest request) {
    var problemDetails =
        new ProblemDetails(Constants.NOT_IMPLEMENTED_TITLE, HttpStatus.NOT_IMPLEMENTED.value());
    problemDetails.setDetail(ex.getMessage());
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }

  /**
   * Turns a body that cannot be read into the request type (malformed JSON, or a value outside an
   * enum such as a lower-case {@code language}) into an RFC 9457 400 problem response. Without this
   * the catch-all below would answer a client mistake with a 500. The parser's message is not
   * echoed: it can repeat the rejected input.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ProblemDetails> handleHttpMessageNotReadableException(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    log.warn("Unreadable request body: {}", ex.getClass().getSimpleName());
    var problemDetails =
        new ProblemDetails(Constants.VALIDATION_FAILED_TITLE, HttpStatus.BAD_REQUEST.value());
    problemDetails.setDetail(Constants.UNREADABLE_REQUEST_DETAIL);
    problemDetails.setInstance(URI.create(request.getRequestURI()));
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }

  /**
   * Fallback for every exception not handled more specifically, so failures (e.g. a database error
   * during save) surface as RFC 9457 Problem Details instead of Spring's default error response. No
   * internal details are leaked in {@code detail}.
   */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ProblemDetails> handleException(Exception ex, HttpServletRequest request) {
    log.error("Unhandled exception while processing request", ex);
    var problemDetails =
        new ProblemDetails(
            Constants.INTERNAL_SERVER_ERROR_TITLE, HttpStatus.INTERNAL_SERVER_ERROR.value());
    problemDetails.setDetail(Constants.INTERNAL_SERVER_ERROR_DETAIL);
    problemDetails.setInstance(URI.create(request.getRequestURI()));

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problemDetails);
  }
}
