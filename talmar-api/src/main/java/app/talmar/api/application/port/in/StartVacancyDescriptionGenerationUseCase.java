package app.talmar.api.application.port.in;

import app.talmar.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import app.talmar.api.domain.generation.VacancyDescriptionGeneration;

public interface StartVacancyDescriptionGenerationUseCase {

  /**
   * Saves a new {@code PENDING} generation and returns it; the draft is produced asynchronously
   * after the transaction commits.
   */
  VacancyDescriptionGeneration start(StartVacancyDescriptionGenerationCommand command);
}
