package app.jobzy.api.application.service;

import app.jobzy.api.application.port.in.CreateVacancyUseCase;
import app.jobzy.api.application.port.in.command.CreateCoreVacancyCommand;
import app.jobzy.api.application.port.out.VacancyRepository;
import app.jobzy.api.domain.vacancy.Vacancy;
import app.jobzy.api.domain.vacancy.valueobject.VacancyStatus;
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
