package app.talmar.api.application.port.in;

import java.util.UUID;

public interface CompleteVacancyDescriptionGenerationUseCase {

  /**
   * Produces the draft for a {@code PENDING} generation and records the outcome.
   *
   * @throws app.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException if the
   *     generation does not exist
   * @throws IllegalStateException if the generation already ended
   */
  void complete(UUID generationId);
}
