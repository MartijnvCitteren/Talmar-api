package eu.taldo.api.application.port.in.vacancy;

import eu.taldo.api.application.port.in.vacancy.command.CreateCoreVacancyCommand;
import eu.taldo.api.domain.vacancy.Vacancy;

public interface CreateVacancyUseCase {

  Vacancy createCoreVacancy(CreateCoreVacancyCommand createCoreVacancyCommand);
}
