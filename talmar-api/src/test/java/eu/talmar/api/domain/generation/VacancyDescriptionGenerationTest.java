package eu.talmar.api.domain.generation;

import static eu.talmar.api.testSupport.GenerationFactory.MODEL;
import static eu.talmar.api.testSupport.GenerationFactory.PROMPT_VERSION;
import static eu.talmar.api.testSupport.GenerationFactory.REQUESTED_AT;
import static eu.talmar.api.testSupport.GenerationFactory.VACANCY_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationStatus;
import eu.talmar.api.testSupport.GenerationFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class VacancyDescriptionGenerationTest {

  @Test
  @DisplayName("given vacancy and input, when new pending then generation is pending with a new id")
  void givenVacancyAndInputWhenNewPendingThenPendingWithNewId() {
    var generation =
        VacancyDescriptionGeneration.newPending(
            VACANCY_ID, GenerationFactory.input(), REQUESTED_AT);

    assertNotNull(generation.getId());
    assertEquals(VACANCY_ID, generation.getVacancyId());
    assertEquals(GenerationFactory.input(), generation.getInput());
    assertEquals(REQUESTED_AT, generation.getRequestedAt());
    assertEquals(GenerationStatus.PENDING, generation.getStatus());
    assertNull(generation.getDraft());
    assertNull(generation.getFailureReason());
    assertNull(generation.getCompletedAt());
  }

  @Test
  @DisplayName("given pending generation, when a draft is accepted then it is completed")
  void givenPendingGenerationWhenDraftAcceptedThenCompleted() {
    var generation = GenerationFactory.pending();
    var completedAt = REQUESTED_AT.plusSeconds(20);

    generation.acceptDraft(GenerationFactory.draft(), MODEL, PROMPT_VERSION, completedAt);

    assertEquals(GenerationStatus.COMPLETED, generation.getStatus());
    assertEquals(GenerationFactory.draft(), generation.getDraft());
    assertEquals(MODEL, generation.getModel());
    assertEquals(PROMPT_VERSION, generation.getPromptVersion());
    assertEquals(completedAt, generation.getCompletedAt());
    assertNull(generation.getFailureReason());
  }

  @Test
  @DisplayName(
      "given pending generation past its deadline, when a draft is accepted then it fails with"
          + " timeout")
  void givenPendingGenerationPastDeadlineWhenDraftAcceptedThenFailsWithTimeout() {
    var generation = GenerationFactory.pending();

    generation.acceptDraft(
        GenerationFactory.draft(), MODEL, PROMPT_VERSION, REQUESTED_AT.plusSeconds(90));

    assertEquals(GenerationStatus.FAILED, generation.getStatus());
    assertEquals(GenerationFailureReason.TIMEOUT, generation.getFailureReason());
    assertNull(generation.getDraft());
  }

  @ParameterizedTest
  @EnumSource(GenerationFailureReason.class)
  @DisplayName("given pending generation, when it fails then it records the reason")
  void givenPendingGenerationWhenFailThenRecordsReason(GenerationFailureReason reason) {
    var generation = GenerationFactory.pending();
    var failedAt = REQUESTED_AT.plusSeconds(5);

    generation.fail(reason, failedAt);

    assertEquals(GenerationStatus.FAILED, generation.getStatus());
    assertEquals(reason, generation.getFailureReason());
    assertEquals(failedAt, generation.getCompletedAt());
  }

  @Test
  @DisplayName(
      "given pending generation past its deadline, when it fails for another reason then the"
          + " reason is timeout")
  void givenPendingGenerationPastDeadlineWhenFailThenReasonIsTimeout() {
    var generation = GenerationFactory.pending();

    generation.fail(GenerationFailureReason.PROVIDER_ERROR, REQUESTED_AT.plusMinutes(5));

    assertEquals(GenerationFailureReason.TIMEOUT, generation.getFailureReason());
  }

  @Test
  @DisplayName("given an ended generation, when it is ended again then it throws")
  void givenEndedGenerationWhenEndedAgainThenThrows() {
    var generation = GenerationFactory.completed();
    var later = REQUESTED_AT.plusSeconds(30);
    var draft = GenerationFactory.draft();

    assertThrows(
        IllegalStateException.class,
        () -> generation.fail(GenerationFailureReason.PROVIDER_ERROR, later));
    assertThrows(
        IllegalStateException.class,
        () -> generation.acceptDraft(draft, MODEL, PROMPT_VERSION, later));
    assertEquals(GenerationStatus.COMPLETED, generation.getStatus());
  }

  @Test
  @DisplayName(
      "given pending generation, when checked just before and at 90 seconds then it times out"
          + " exactly at the deadline")
  void givenPendingGenerationWhenCheckedAroundDeadlineThenTimesOutAtDeadline() {
    var generation = GenerationFactory.pending();

    assertFalse(generation.isTimedOutAt(REQUESTED_AT.plusSeconds(89)));
    assertTrue(generation.isTimedOutAt(REQUESTED_AT.plusSeconds(90)));
  }

  @Test
  @DisplayName("given a completed generation, when checked long after then it is not timed out")
  void givenCompletedGenerationWhenCheckedLongAfterThenNotTimedOut() {
    assertFalse(GenerationFactory.completed().isTimedOutAt(REQUESTED_AT.plusDays(1)));
  }

  @Test
  @DisplayName("given two generations with the same id, when compared then they are equal")
  void givenSameIdWhenEqualsThenTrue() {
    var generation = GenerationFactory.pending();
    var copy =
        VacancyDescriptionGeneration.builder()
            .id(generation.getId())
            .vacancyId(VACANCY_ID)
            .input(GenerationFactory.input())
            .requestedAt(REQUESTED_AT)
            .status(GenerationStatus.PENDING)
            .build();

    assertEquals(generation, copy);
    assertEquals(generation.hashCode(), copy.hashCode());
  }
}
