package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.SetManualVacancyDescriptionCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface ManualVacancyDescriptionUseCase {

  Vacancy setManualDescription(SetManualVacancyDescriptionCommand command);
}
