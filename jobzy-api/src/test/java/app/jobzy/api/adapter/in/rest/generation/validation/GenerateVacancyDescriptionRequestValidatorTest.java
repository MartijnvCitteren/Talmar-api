package app.jobzy.api.adapter.in.rest.generation.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.jobzy.api.adapter.in.rest.generation.InvalidGenerationRequestException;
import app.jobzy.api.vacancy.adapter.in.web.contract.GenerateVacancyDescriptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class GenerateVacancyDescriptionRequestValidatorTest {

  private final GenerateVacancyDescriptionRequestValidator validator =
      new GenerateVacancyDescriptionRequestValidator();

  @Test
  @DisplayName("given plain answers with newlines, when validate then it passes")
  void givenPlainAnswersWhenValidateThenPasses() {
    var request =
        new GenerateVacancyDescriptionRequest("Sell\n- and advise", "Five people", "Freedom");

    assertDoesNotThrow(() -> validator.validate(request));
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
    "mostImportantTasks, <script>alert(1)</script>, raw HTML or script-like tags",
    "team, '<img src=x onerror=y>', raw HTML or script-like tags",
    "whyNiceJob, 'Bell\u0007', disallowed control characters"
  })
  @DisplayName("given an unsafe answer, when validate then it is rejected naming the field")
  void givenUnsafeAnswerWhenValidateThenRejectedNamingField(
      String field, String value, String expectedMessagePart) {
    var request = new GenerateVacancyDescriptionRequest("Sell", "Five people", "Freedom");
    switch (field) {
      case "mostImportantTasks" -> request.setMostImportantTasks(value);
      case "team" -> request.setTeam(value);
      default -> request.setWhyNiceJob(value);
    }

    var exception =
        assertThrows(InvalidGenerationRequestException.class, () -> validator.validate(request));

    assertEquals(field, exception.getField());
    assertEquals(field + " must not contain " + expectedMessagePart, exception.getMessage());
  }
}
