package eu.talmar.api.adapter.in.rest.vacancy.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyDescriptionRequestException;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class VacancyDescriptionContentValidatorTest {

  private final VacancyDescriptionContentValidator validator =
      new VacancyDescriptionContentValidator();

  @Test
  @DisplayName("given plain multi-line text in every field, when validate then does not throw")
  void givenPlainMultiLineTextInEveryFieldWhenValidateThenDoesNotThrow() {
    var request =
        new VacancyDescriptionRequest()
            .summary("Line one\nLine two")
            .jobDescription("Tab\tseparated")
            .tasks("Carriage\rreturn")
            .whatWeOffer("Plain text")
            .aboutUs("More plain text");

    assertDoesNotThrow(() -> validator.validate(request));
  }

  @Test
  @DisplayName("given a request with all fields null, when validate then does not throw")
  void givenRequestWithAllFieldsNullWhenValidateThenDoesNotThrow() {
    assertDoesNotThrow(() -> validator.validate(new VacancyDescriptionRequest()));
  }

  @Test
  @DisplayName(
      "given text containing a lone less-than sign not forming a tag, when validate then does not"
          + " throw")
  void givenLoneLessThanSignNotFormingTagWhenValidateThenDoesNotThrow() {
    var request = new VacancyDescriptionRequest().summary("revenue < target");

    assertDoesNotThrow(() -> validator.validate(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"<script>alert(1)</script>", "<b>bold</b>", "</div>", "<!--comment-->"})
  @DisplayName("given raw HTML or script-like content, when validate then rejected")
  void givenRawHtmlOrScriptLikeContentWhenValidateThenRejected(String maliciousContent) {
    var request = new VacancyDescriptionRequest().summary(maliciousContent);

    assertThrows(
        InvalidVacancyDescriptionRequestException.class, () -> validator.validate(request));
  }

  @Test
  @DisplayName("given a disallowed control character, when validate then rejected")
  void givenDisallowedControlCharacterWhenValidateThenRejected() {
    var request = new VacancyDescriptionRequest().summary("Summary with  bell character");

    assertThrows(
        InvalidVacancyDescriptionRequestException.class, () -> validator.validate(request));
  }

  @ParameterizedTest
  @MethodSource("fieldSetters")
  @DisplayName("given invalid content in a single field, when validate then names that field")
  void givenInvalidContentInSingleFieldWhenValidateThenNamesThatField(
      String fieldName, Function<VacancyDescriptionRequest, VacancyDescriptionRequest> setter) {
    var request = setter.apply(new VacancyDescriptionRequest());

    var exception =
        assertThrows(
            InvalidVacancyDescriptionRequestException.class, () -> validator.validate(request));

    assertEquals(fieldName, exception.getField());
  }

  private static Stream<Arguments> fieldSetters() {
    String tag = "<script>";
    return Stream.of(
        Arguments.of(
            "summary",
            (Function<VacancyDescriptionRequest, VacancyDescriptionRequest>) r -> r.summary(tag)),
        Arguments.of(
            "jobDescription",
            (Function<VacancyDescriptionRequest, VacancyDescriptionRequest>)
                r -> r.jobDescription(tag)),
        Arguments.of(
            "tasks",
            (Function<VacancyDescriptionRequest, VacancyDescriptionRequest>) r -> r.tasks(tag)),
        Arguments.of(
            "whatWeOffer",
            (Function<VacancyDescriptionRequest, VacancyDescriptionRequest>)
                r -> r.whatWeOffer(tag)),
        Arguments.of(
            "aboutUs",
            (Function<VacancyDescriptionRequest, VacancyDescriptionRequest>) r -> r.aboutUs(tag)));
  }
}
