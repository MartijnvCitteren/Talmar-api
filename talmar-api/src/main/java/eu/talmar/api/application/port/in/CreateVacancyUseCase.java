package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface CreateVacancyUseCase {

  Vacancy createCoreVacancy(CreateCoreVacancyCommand createCoreVacancyCommand);
}
