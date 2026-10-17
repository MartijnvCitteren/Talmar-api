package app.talmar.api.application.port.in;

import app.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import app.talmar.api.domain.vacancy.Vacancy;

public interface CreateVacancyUseCase {

  Vacancy createCoreVacancy(CreateCoreVacancyCommand createCoreVacancyCommand);
}
