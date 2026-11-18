package eu.talmar.api.application.port.in.vacancy;

import eu.talmar.api.application.port.in.vacancy.command.UpdateVacancyCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface UpdateVacancyUseCase {

  Vacancy updateVacancy(UpdateVacancyCommand command);
}
