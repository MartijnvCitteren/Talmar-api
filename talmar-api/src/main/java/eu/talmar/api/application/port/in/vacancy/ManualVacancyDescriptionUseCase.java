package eu.talmar.api.application.port.in.vacancy;

import eu.talmar.api.application.port.in.vacancy.command.SetManualVacancyDescriptionCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface ManualVacancyDescriptionUseCase {

  Vacancy setManualDescription(SetManualVacancyDescriptionCommand command);
}
