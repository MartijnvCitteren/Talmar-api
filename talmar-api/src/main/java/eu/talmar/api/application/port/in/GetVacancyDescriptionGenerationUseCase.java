package eu.talmar.api.application.port.in;

import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationFailedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationRejectedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationUnavailableException;
import java.util.UUID;

public interface GetVacancyDescriptionGenerationUseCase {

  /**
   * Returns a generation that is still {@code PENDING} or has {@code COMPLETED}. A generation that
   * failed, or is {@code PENDING} past its deadline, is reported as the exception for its failure.
   *
   * @throws VacancyDescriptionGenerationNotFoundException if the generation does not exist for this
   *     vacancy
   * @throws VacancyDescriptionGenerationRejectedException if the model rejected the input
   * @throws VacancyDescriptionGenerationUnavailableException if the AI provider refused the request
   *     in a way a retry does not fix
   * @throws VacancyDescriptionGenerationFailedException for every other failure, including the
   *     timeout
   */
  VacancyDescriptionGeneration get(UUID vacancyId, UUID generationId);
}
