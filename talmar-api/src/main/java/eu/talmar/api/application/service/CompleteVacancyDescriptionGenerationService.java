package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.CompleteVacancyDescriptionGenerationUseCase;
import eu.talmar.api.application.port.out.GenerationResult;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerator;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Produces the draft for a {@code PENDING} generation and records the outcome.
 *
 * <p>Deliberately not {@code @Transactional}: it runs on a background thread after the starting
 * transaction committed, and loading and saving are each their own short transaction (the
 * repository's), so no database connection is held while waiting up to a minute on the AI provider.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class CompleteVacancyDescriptionGenerationService
    implements CompleteVacancyDescriptionGenerationUseCase {
  private final VacancyDescriptionGenerationRepository generationRepository;
  private final VacancyDescriptionGenerator generator;
  private final Clock clock;

  @Override
  public void complete(UUID generationId) {
    var generation = getGeneration(generationId);

    var result = generateSafely(generation.getInput());
    var now = LocalDateTime.now(clock);
    switch (result) {
      case GenerationResult.Drafted(var draft, var model, var promptVersion) ->
          generation.acceptDraft(draft, model, promptVersion, now);
      case GenerationResult.Rejected(var reason) -> generation.fail(reason, now);
    }

    generationRepository.save(generation);
    log.info("Description generation is completed: {}", generation);
  }

  private VacancyDescriptionGeneration getGeneration(UUID generationId) {
    return generationRepository
        .findById(generationId)
        .orElseThrow(() -> new VacancyDescriptionGenerationNotFoundException(generationId));
  }

  /**
   * The port promises not to throw, but this runs on a background thread where an exception would
   * only be logged and leave the generation {@code PENDING} until it times out. Recording it as a
   * provider error gives the user a definite answer right away.
   */
  private GenerationResult generateSafely(GenerationInput input) {
    try {
      return generator.generate(input);
    } catch (RuntimeException e) {
      log.error("Description generator failed unexpectedly", e);
      return new GenerationResult.Rejected(GenerationFailureReason.PROVIDER_ERROR);
    }
  }
}
