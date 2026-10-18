package eu.talmar.api.adapter.out.ai.mistral;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GeneratedDraftValidatorTest {

  @Test
  @DisplayName(
      "given texts exactly at their maximum lengths with newlines, when validated then it passes")
  void givenTextsAtMaximumLengthsWhenValidatedThenPasses() {
    var draft =
        new GeneratedDraft(
            "s".repeat(GeneratedDraftValidator.SUMMARY_MAX_LENGTH),
            "j\n".repeat(GeneratedDraftValidator.JOB_DESCRIPTION_MAX_LENGTH / 2),
            "t".repeat(GeneratedDraftValidator.TASKS_MAX_LENGTH));

    assertDoesNotThrow(() -> GeneratedDraftValidator.validate(draft));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unsaveableDrafts")
  @DisplayName("given a draft breaking a rule, when validated then it is invalid output")
  void givenDraftBreakingRuleWhenValidatedThenInvalidOutput(String rule, GeneratedDraft draft) {
    assertThrows(InvalidModelOutputException.class, () -> GeneratedDraftValidator.validate(draft));
  }

  private static Stream<Arguments> unsaveableDrafts() {
    return Stream.of(
        arguments("missing job description", new GeneratedDraft("Summary", null, "Tasks")),
        arguments("blank summary", new GeneratedDraft(" ", "Job", "Tasks")),
        arguments("empty tasks", new GeneratedDraft("Summary", "Job", "")),
        arguments("summary too long", new GeneratedDraft("s".repeat(1001), "Job", "Tasks")),
        arguments("job description too long", new GeneratedDraft("S", "j".repeat(5001), "T")),
        arguments("tasks too long", new GeneratedDraft("S", "J", "t".repeat(5001))),
        arguments("raw tag", new GeneratedDraft("S", "<a href='x'>link</a>", "T")),
        arguments("control character", new GeneratedDraft("S", "J", "Task\u0000")));
  }
}
