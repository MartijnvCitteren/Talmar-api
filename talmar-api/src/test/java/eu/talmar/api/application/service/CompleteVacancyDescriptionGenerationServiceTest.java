package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.GenerationFactory.MODEL;
import static eu.talmar.api.testSupport.GenerationFactory.PROMPT_VERSION;
import static eu.talmar.api.testSupport.GenerationFactory.REQUESTED_AT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.out.GenerationResult;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerator;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.testSupport.GenerationFactory;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompleteVacancyDescriptionGenerationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(REQUESTED_AT.plusSeconds(20).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  @Mock private VacancyDescriptionGenerationRepository generationRepository;
  @Mock private VacancyDescriptionGenerator generator;

  private CompleteVacancyDescriptionGenerationService service;

  @BeforeEach
  void setUp() {
    service =
        new CompleteVacancyDescriptionGenerationService(generationRepository, generator, CLOCK);
  }

  @Test
  @DisplayName("given a pending generation and a draft, when complete then the generation is saved")
  void givenPendingGenerationAndDraftWhenCompleteThenSaved() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    when(generator.generate(generation.getInput()))
        .thenReturn(new GenerationResult.Drafted(GenerationFactory.draft(), MODEL, PROMPT_VERSION));

    service.complete(generation.getId());

    verify(generationRepository).save(generation);
  }

  @Test
  @DisplayName(
      "given a pending generation and a rejection, when complete then the generation is saved")
  void givenPendingGenerationAndRejectionWhenCompleteThenSaved() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    when(generator.generate(generation.getInput()))
        .thenReturn(new GenerationResult.Rejected(GenerationFailureReason.INPUT_REJECTED));

    service.complete(generation.getId());

    verify(generationRepository).save(generation);
  }

  @Test
  @DisplayName(
      "given the generator throws, when complete then the generation is saved as a provider"
          + " error")
  void givenGeneratorThrowsWhenCompleteThenSavedAsProviderError() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    when(generator.generate(any())).thenThrow(new IllegalStateException("unexpected"));

    service.complete(generation.getId());

    assertEquals(GenerationFailureReason.PROVIDER_ERROR, generation.getFailureReason());
    verify(generationRepository).save(generation);
  }

  @Test
  @DisplayName(
      "given a generation that does not exist, when complete then it throws and the generator is"
          + " not called")
  void givenMissingGenerationWhenCompleteThenThrowsAndGeneratorNotCalled() {
    var generationId = UUID.randomUUID();
    when(generationRepository.findById(generationId)).thenReturn(Optional.empty());

    assertThrows(
        VacancyDescriptionGenerationNotFoundException.class, () -> service.complete(generationId));

    verify(generator, never()).generate(any());
  }
}
