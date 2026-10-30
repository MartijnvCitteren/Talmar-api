package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.UpdateVacancyCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface UpdateVacancyUseCase {

  Vacancy updateVacancy(UpdateVacancyCommand command);
}
