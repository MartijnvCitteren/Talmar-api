package eu.taldo.api.application.port.in.vacancy;

import eu.taldo.api.application.port.in.vacancy.command.UpdateVacancyCommand;
import eu.taldo.api.domain.vacancy.Vacancy;

public interface UpdateVacancyUseCase {

  Vacancy updateVacancy(UpdateVacancyCommand command);
}
