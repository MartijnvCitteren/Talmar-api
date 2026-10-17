package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;

public interface StartVacancyDescriptionGenerationUseCase {

  /**
   * Saves a new {@code PENDING} generation and returns it; the draft is produced asynchronously
   * after the transaction commits.
   */
  VacancyDescriptionGeneration start(StartVacancyDescriptionGenerationCommand command);
}
