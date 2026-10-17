package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.CreateVacancyUseCase;
import eu.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.valueobject.VacancyStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class CreateVacancyService implements CreateVacancyUseCase {
  private final VacancyRepository vacancyRepository;
  private final Clock clock;

  @Override
  public Vacancy createCoreVacancy(CreateCoreVacancyCommand command) {
    var vacancy =
        Vacancy.builder()
            .jobTitle(command.jobTitle())
            .language(command.language())
            .location(command.location())
            .category(command.category())
            .status(VacancyStatus.DRAFT)
            .hoursPerWeek(command.hoursPerWeek())
            .workplaceType(command.workplaceType())
            .createdAt(LocalDateTime.now(clock))
            .build();

    log.info("Vacancy is created: {}", vacancy);
    vacancyRepository.save(vacancy);

    return vacancy;
  }
}
