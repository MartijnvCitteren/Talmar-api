package eu.talmar.api.adapter.in.rest.vacancy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;

class VacancyExceptionHandlerTest {

  private final VacancyExceptionHandler handler = new VacancyExceptionHandler();

  @Test
  @DisplayName(
      "given a vacancy not found exception, when handling then returns a 404 problem detail")
  void givenVacancyNotFoundExceptionWhenHandleThenReturns404ProblemDetail() {
    var id = UUID.randomUUID();

    var response =
        handler.handleVacancyNotFoundException(new VacancyNotFoundException(id), request());

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
    assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().getStatus());
  }

  @Test
  @DisplayName(
      "given an invalid vacancy description request exception, when handling then returns a 400"
          + " problem detail naming the offending field")
  void givenInvalidVacancyDescriptionRequestExceptionWhenHandleThenReturns400ProblemDetail() {
    var exception =
        new InvalidVacancyDescriptionRequestException("summary", "summary contains a raw tag");

    var response = handler.handleInvalidVacancyDescriptionRequestException(exception, request());

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
    assertEquals(1, response.getBody().getErrors().size());
    assertEquals("summary", response.getBody().getErrors().getFirst().getField());
    assertEquals(
        "summary contains a raw tag", response.getBody().getErrors().getFirst().getMessage());
  }

  private MockHttpServletRequest request() {
    return new MockHttpServletRequest("POST", "/api/v1/vacancy");
  }
}
