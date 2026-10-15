package app.jobzy.api.application.port.in;

import app.jobzy.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import app.jobzy.api.domain.generation.VacancyDescriptionGeneration;

public interface StartVacancyDescriptionGenerationUseCase {

  /**
   * Saves a new {@code PENDING} generation and returns it; the draft is produced asynchronously
   * after the transaction commits.
   */
  VacancyDescriptionGeneration start(StartVacancyDescriptionGenerationCommand command);
}
