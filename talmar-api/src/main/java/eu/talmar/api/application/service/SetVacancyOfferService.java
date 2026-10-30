package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.SetVacancyOfferUseCase;
import eu.talmar.api.application.port.in.command.SetVacancyOfferCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the contact person and offer of a vacancy. Both are always replaced as a whole, so a
 * field the user cleared in the form is cleared here too.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class SetVacancyOfferService implements SetVacancyOfferUseCase {
  private final VacancyRepository vacancyRepository;

  @Override
  @Transactional
  public Vacancy setOffer(SetVacancyOfferCommand command) {
    Vacancy vacancy =
        vacancyRepository
            .findById(command.vacancyId())
            .orElseThrow(() -> new VacancyNotFoundException(command.vacancyId()));

    vacancy.setContactPerson(command.contactPerson());
    vacancy.setOffer(command.offer());

    log.info("Contact person and offer are set for vacancy: {}", vacancy.getId());
    vacancyRepository.save(vacancy);

    return vacancy;
  }
}
