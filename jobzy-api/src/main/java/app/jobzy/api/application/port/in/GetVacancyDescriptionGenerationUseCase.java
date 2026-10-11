package app.jobzy.api.application.port.in;

import app.jobzy.api.domain.generation.VacancyDescriptionGeneration;
import java.util.UUID;

public interface GetVacancyDescriptionGenerationUseCase {

  /**
   * Returns a generation that is still {@code PENDING} or has {@code COMPLETED}. A generation that
   * failed, or is {@code PENDING} past its deadline, is reported as the exception for its failure.
   *
   * @throws app.jobzy.api.domain.generation.VacancyDescriptionGenerationNotFoundException if the
   *     generation does not exist for this vacancy
   * @throws app.jobzy.api.domain.generation.VacancyDescriptionGenerationRejectedException if the
   *     model rejected the input
   * @throws app.jobzy.api.domain.generation.VacancyDescriptionGenerationUnavailableException if the
   *     AI provider refused the request in a way a retry does not fix
   * @throws app.jobzy.api.domain.generation.VacancyDescriptionGenerationFailedException for every
   *     other failure, including the timeout
   */
  VacancyDescriptionGeneration get(UUID vacancyId, UUID generationId);
}
