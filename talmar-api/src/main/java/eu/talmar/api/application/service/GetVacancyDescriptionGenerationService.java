package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.GetVacancyDescriptionGenerationUseCase;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationFailedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationRejectedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationUnavailableException;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Answers a poll. A generation is only found through the vacancy it was started for, so a
 * generation id cannot be used to read a draft via another vacancy's URL.
 *
 * <p>A failed generation is reported as an exception per kind of failure, so the REST adapter maps
 * each to its response without looking at the failure reason. A {@code PENDING} generation past its
 * deadline counts as a {@code TIMEOUT}: the work on it was lost (crash or restart) and nobody will
 * finish it. The stored row stays {@code PENDING} until the retention clean-up removes it.
 */
@Service
@RequiredArgsConstructor
public class GetVacancyDescriptionGenerationService
    implements GetVacancyDescriptionGenerationUseCase {
  private final VacancyDescriptionGenerationRepository generationRepository;
  private final Clock clock;

  @Override
  public VacancyDescriptionGeneration get(UUID vacancyId, UUID generationId) {
    var generation = getGenerationOfVacancy(vacancyId, generationId);
    throwIfFailed(generation);
    return generation;
  }

  private VacancyDescriptionGeneration getGenerationOfVacancy(UUID vacancyId, UUID generationId) {
    return generationRepository
        .findById(generationId)
        .filter(generation -> generation.getVacancyId().equals(vacancyId))
        .orElseThrow(
            () -> new VacancyDescriptionGenerationNotFoundException(vacancyId, generationId));
  }

  private void throwIfFailed(VacancyDescriptionGeneration generation) {
    if (generation.isTimedOutAt(LocalDateTime.now(clock))) {
      throw new VacancyDescriptionGenerationFailedException(
          generation.getId(), GenerationFailureReason.TIMEOUT);
    }
    if (generation.getStatus() == GenerationStatus.FAILED) {
      throw failureOf(generation);
    }
  }

  private static RuntimeException failureOf(VacancyDescriptionGeneration generation) {
    var reason = generation.getFailureReason();
    return switch (reason) {
      case INPUT_REJECTED -> new VacancyDescriptionGenerationRejectedException(generation.getId());
      case PROVIDER_UNAVAILABLE ->
          new VacancyDescriptionGenerationUnavailableException(generation.getId());
      case PROVIDER_ERROR, TIMEOUT, INVALID_OUTPUT ->
          new VacancyDescriptionGenerationFailedException(generation.getId(), reason);
    };
  }
}
