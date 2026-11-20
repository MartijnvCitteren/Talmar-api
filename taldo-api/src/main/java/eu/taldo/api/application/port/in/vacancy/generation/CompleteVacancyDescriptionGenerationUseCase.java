package eu.taldo.api.application.port.in.vacancy.generation;

import eu.taldo.api.domain.vacancy.generation.exception.VacancyDescriptionGenerationNotFoundException;
import java.util.UUID;

public interface CompleteVacancyDescriptionGenerationUseCase {

  /**
   * Produces the draft for a {@code PENDING} generation and records the outcome.
   *
   * @throws VacancyDescriptionGenerationNotFoundException if the generation does not exist
   * @throws IllegalStateException if the generation already ended
   */
  void complete(UUID generationId);
}
