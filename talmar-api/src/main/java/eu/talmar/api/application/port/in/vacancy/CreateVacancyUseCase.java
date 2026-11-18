package eu.talmar.api.application.port.in.vacancy;

import eu.talmar.api.application.port.in.vacancy.command.CreateCoreVacancyCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface CreateVacancyUseCase {

  Vacancy createCoreVacancy(CreateCoreVacancyCommand createCoreVacancyCommand);
}
