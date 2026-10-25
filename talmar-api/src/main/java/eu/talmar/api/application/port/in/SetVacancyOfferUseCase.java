package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.SetVacancyOfferCommand;
import eu.talmar.api.domain.vacancy.Vacancy;

public interface SetVacancyOfferUseCase {

  Vacancy setOffer(SetVacancyOfferCommand command);
}
