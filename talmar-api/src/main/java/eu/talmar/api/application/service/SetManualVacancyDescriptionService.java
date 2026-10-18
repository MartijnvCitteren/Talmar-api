package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.ManualVacancyDescriptionUseCase;
import eu.talmar.api.application.port.in.command.SetManualVacancyDescriptionCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
@RequiredArgsConstructor
public class SetManualVacancyDescriptionService implements ManualVacancyDescriptionUseCase {
  private final VacancyRepository vacancyRepository;

  @Override
  @Transactional
  public Vacancy setManualDescription(SetManualVacancyDescriptionCommand command) {
    Vacancy vacancy =
        vacancyRepository
            .findById(command.vacancyId())
            .orElseThrow(() -> new VacancyNotFoundException(command.vacancyId()));

    vacancy.setDescription(
        new VacancyDescription(
            command.summary(),
            command.jobDescription(),
            command.tasks(),
            command.whatWeOffer(),
            command.aboutUs(),
            VacancyDescriptionSource.MANUAL));

    log.info("Manual description is set for vacancy: {}", vacancy.getId());
    vacancyRepository.save(vacancy);

    return vacancy;
  }
}
