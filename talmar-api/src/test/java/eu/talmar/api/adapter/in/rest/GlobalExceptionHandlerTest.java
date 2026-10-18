package eu.talmar.api.adapter.in.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetailsErrorsInner;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyCoreRequest;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName(
      "given field errors, when handling the exception, then the problem detail lists every invalid"
          + " field with its constraint message")
  void givenFieldErrorsWhenHandleThenProblemDetailsListsEveryInvalidField() {
    var bindingResult = bindingResult();
    bindingResult.rejectValue("jobTitle", "Size", "size must be between 0 and 200");
    bindingResult.rejectValue("minHoursPerWeek", "Min", "must be greater than 0");

    var response =
        handler.handleMethodArgumentNotValidException(exception(bindingResult), request());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());

    ProblemDetails body = response.getBody();
    assertEquals(URI.create("about:blank"), body.getType());
    assertEquals("Validation failed", body.getTitle());
    assertEquals(HttpStatus.BAD_REQUEST.value(), body.getStatus());
    assertEquals(URI.create("/api/v1/vacancy"), body.getInstance());
    assertEquals(
        "The request failed validation on one or more fields, see 'errors' for details.",
        body.getDetail());

    assertEquals(
        List.of("jobTitle", "minHoursPerWeek"),
        body.getErrors().stream().map(ProblemDetailsErrorsInner::getField).toList());
    assertEquals("size must be between 0 and 200", messageFor(body, "jobTitle"));
    assertEquals("must be greater than 0", messageFor(body, "minHoursPerWeek"));
  }

  @Test
  @DisplayName(
      "given a class-level error, when handling the exception, then it is reported under the object"
          + " name")
  void givenClassLevelErrorWhenHandleThenReportedUnderObjectName() {
    var bindingResult = bindingResult();
    bindingResult.reject("HoursRange", "maxHoursPerWeek must be greater than minHoursPerWeek");

    var body =
        handler
            .handleMethodArgumentNotValidException(exception(bindingResult), request())
            .getBody();

    assertEquals(1, body.getErrors().size());
    assertEquals("vacancyCoreRequest", body.getErrors().getFirst().getField());
    assertEquals(
        "maxHoursPerWeek must be greater than minHoursPerWeek",
        body.getErrors().getFirst().getMessage());
  }

  @Test
  @DisplayName(
      "given an operation that is not built yet, when handling then returns a 501 problem detail"
          + " naming the operation")
  void givenEndpointNotImplementedWhenHandleThenReturns501ProblemDetailNamingTheOperation() {
    var response =
        handler.handleEndpointNotImplementedException(
            new EndpointNotImplementedException("getVacancy"), request());

    assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());

    ProblemDetails body = response.getBody();
    assertEquals("Not implemented", body.getTitle());
    assertEquals(HttpStatus.NOT_IMPLEMENTED.value(), body.getStatus());
    assertEquals("Operation is not implemented yet: getVacancy", body.getDetail());
    assertEquals(URI.create("/api/v1/vacancy"), body.getInstance());
  }

  @Test
  @DisplayName("given an unexpected exception, when handling then returns a 500 problem detail")
  void givenUnexpectedExceptionWhenHandleThenReturns500ProblemDetail() {
    var response = handler.handleException(new RuntimeException("boom"), request());

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().getStatus());
  }

  private BeanPropertyBindingResult bindingResult() {
    return new BeanPropertyBindingResult(new VacancyCoreRequest(), "vacancyCoreRequest");
  }

  private MethodArgumentNotValidException exception(BindingResult bindingResult) {
    return new MethodArgumentNotValidException(methodParameter(), bindingResult);
  }

  private MethodParameter methodParameter() {
    try {
      return new MethodParameter(
          GlobalExceptionHandlerTest.class.getDeclaredMethod("validated", VacancyCoreRequest.class),
          0);
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unused")
  private void validated(VacancyCoreRequest request) {}

  private MockHttpServletRequest request() {
    return new MockHttpServletRequest("POST", "/api/v1/vacancy");
  }

  private String messageFor(ProblemDetails body, String field) {
    return body.getErrors().stream()
        .filter(error -> field.equals(error.getField()))
        .findFirst()
        .orElseThrow()
        .getMessage();
  }
}
