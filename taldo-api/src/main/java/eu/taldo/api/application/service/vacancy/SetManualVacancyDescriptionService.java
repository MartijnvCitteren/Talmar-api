package eu.taldo.api.application.service.vacancy;

import eu.taldo.api.application.port.in.vacancy.ManualVacancyDescriptionUseCase;
import eu.taldo.api.application.port.in.vacancy.command.SetManualVacancyDescriptionCommand;
import eu.taldo.api.application.port.out.vacancy.VacancyRepository;
import eu.taldo.api.domain.vacancy.Vacancy;
import eu.taldo.api.domain.vacancy.exception.VacancyNotFoundException;
import eu.taldo.api.domain.vacancy.valueobject.VacancyDescription;
import eu.taldo.api.domain.vacancy.valueobject.VacancyDescriptionSource;
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
