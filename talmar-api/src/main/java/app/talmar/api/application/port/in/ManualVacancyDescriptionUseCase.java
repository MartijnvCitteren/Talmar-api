package app.talmar.api.application.port.in;

import app.talmar.api.application.port.in.command.SetManualVacancyDescriptionCommand;
import app.talmar.api.domain.vacancy.Vacancy;

public interface ManualVacancyDescriptionUseCase {

  Vacancy setManualDescription(SetManualVacancyDescriptionCommand command);
}
