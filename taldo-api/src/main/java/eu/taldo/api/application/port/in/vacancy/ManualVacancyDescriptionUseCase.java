package eu.taldo.api.application.port.in.vacancy;

import eu.taldo.api.application.port.in.vacancy.command.SetManualVacancyDescriptionCommand;
import eu.taldo.api.domain.vacancy.Vacancy;

public interface ManualVacancyDescriptionUseCase {

  Vacancy setManualDescription(SetManualVacancyDescriptionCommand command);
}
