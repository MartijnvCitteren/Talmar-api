package eu.taldo.api.application.port.in.vacancy.generation;

import eu.taldo.api.application.port.in.vacancy.generation.command.StartVacancyDescriptionGenerationCommand;
import eu.taldo.api.domain.vacancy.generation.VacancyDescriptionGeneration;

public interface StartVacancyDescriptionGenerationUseCase {

  /**
   * Saves a new {@code PENDING} generation and returns it; the draft is produced asynchronously
   * after the transaction commits.
   */
  VacancyDescriptionGeneration start(StartVacancyDescriptionGenerationCommand command);
}
