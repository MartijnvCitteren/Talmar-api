package eu.talmar.api.adapter.out.persistence.generation;

import static eu.talmar.api.testSupport.GenerationFactory.REQUESTED_AT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.adapter.out.persistence.generation.mapper.VacancyDescriptionGenerationJpaMapperImpl;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.testSupport.GenerationFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import({
  VacancyDescriptionGenerationRepositoryAdapter.class,
  VacancyDescriptionGenerationJpaMapperImpl.class
})
class VacancyDescriptionGenerationRepositoryAdapterTest {

  @Autowired private VacancyDescriptionGenerationRepositoryAdapter adapter;
  @Autowired private TestEntityManager entityManager;

  @Test
  @DisplayName(
      "given a completed generation with a draft at maximum lengths, when saved and reloaded then"
          + " every field round-trips")
  void givenCompletedGenerationWhenSavedAndReloadedThenEveryFieldRoundTrips() {
    var draft = new GeneratedDraft("s".repeat(1000), "j".repeat(5000), "t".repeat(5000));
    var generation = GenerationFactory.pending();
    generation.acceptDraft(
        draft,
        GenerationFactory.MODEL,
        GenerationFactory.PROMPT_VERSION,
        REQUESTED_AT.plusSeconds(10));

    var reloaded = saveAndReload(generation);

    assertEquals(generation.getId(), reloaded.getId());
    assertEquals(generation.getVacancyId(), reloaded.getVacancyId());
    assertEquals(generation.getInput(), reloaded.getInput());
    assertEquals(generation.getStatus(), reloaded.getStatus());
    assertEquals(draft, reloaded.getDraft());
    assertEquals(GenerationFactory.MODEL, reloaded.getModel());
    assertEquals(GenerationFactory.PROMPT_VERSION, reloaded.getPromptVersion());
    assertEquals(generation.getRequestedAt(), reloaded.getRequestedAt());
    assertEquals(generation.getCompletedAt(), reloaded.getCompletedAt());
  }

  @Test
  @DisplayName(
      "given a pending generation saved, when it fails and is saved again then the stored row is"
          + " updated")
  void givenPendingGenerationWhenFailedAndSavedAgainThenRowUpdated() {
    var generation = GenerationFactory.pending();
    adapter.save(generation);
    generation.fail(GenerationFailureReason.PROVIDER_ERROR, REQUESTED_AT.plusSeconds(5));

    var reloaded = saveAndReload(generation);

    assertEquals(GenerationFailureReason.PROVIDER_ERROR, reloaded.getFailureReason());
    assertNull(reloaded.getDraft());
  }

  @Test
  @DisplayName(
      "given generations before and after a cutoff, when deleting before the cutoff then only the"
          + " older ones are removed")
  void givenGenerationsAroundCutoffWhenDeleteRequestedBeforeThenOnlyOlderRemoved() {
    var old =
        VacancyDescriptionGeneration.newPending(
            GenerationFactory.VACANCY_ID, GenerationFactory.input(), REQUESTED_AT);
    var atCutoff =
        VacancyDescriptionGeneration.newPending(
            GenerationFactory.VACANCY_ID, GenerationFactory.input(), REQUESTED_AT.plusHours(1));
    adapter.save(old);
    adapter.save(atCutoff);
    entityManager.flush();

    var deleted = adapter.deleteRequestedBefore(REQUESTED_AT.plusHours(1));
    entityManager.clear();

    assertEquals(1, deleted);
    assertTrue(adapter.findById(old.getId()).isEmpty());
    assertTrue(adapter.findById(atCutoff.getId()).isPresent());
  }

  private VacancyDescriptionGeneration saveAndReload(VacancyDescriptionGeneration generation) {
    adapter.save(generation);
    entityManager.flush();
    entityManager.clear();
    return adapter.findById(generation.getId()).orElseThrow();
  }
}
