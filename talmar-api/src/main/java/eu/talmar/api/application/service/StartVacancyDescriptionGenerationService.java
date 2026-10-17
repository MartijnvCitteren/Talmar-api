package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.StartVacancyDescriptionGenerationUseCase;
import eu.talmar.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationStarted;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Starts a description generation: snapshots the vacancy's job title and language together with the
 * user's answers, saves the generation as {@code PENDING} and publishes {@link
 * VacancyDescriptionGenerationStarted}. The draft itself is produced by {@code
 * VacancyDescriptionGenerationStartedListener} after this transaction commits, so the {@code
 * PENDING} row is visible to a poll before any work on it starts.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class StartVacancyDescriptionGenerationService
    implements StartVacancyDescriptionGenerationUseCase {
  private final VacancyRepository vacancyRepository;
  private final VacancyDescriptionGenerationRepository generationRepository;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  @Override
  @Transactional
  public VacancyDescriptionGeneration start(StartVacancyDescriptionGenerationCommand command) {
    var vacancy = getVacancy(command.vacancyId());
    var pendingGeneration =
        VacancyDescriptionGeneration.newPending(
            vacancy.getId(), toGenerationInput(vacancy, command), LocalDateTime.now(clock));

    generationRepository.save(pendingGeneration);
    eventPublisher.publishEvent(new VacancyDescriptionGenerationStarted(pendingGeneration.getId()));
    log.info("Description generation is started: {}", pendingGeneration);

    return pendingGeneration;
  }

  private Vacancy getVacancy(UUID vacancyId) {
    return vacancyRepository
        .findById(vacancyId)
        .orElseThrow(() -> new VacancyNotFoundException(vacancyId));
  }

  private static GenerationInput toGenerationInput(
      Vacancy vacancy, StartVacancyDescriptionGenerationCommand command) {
    return new GenerationInput(
        vacancy.getJobTitle(),
        vacancy.getLanguage(),
        command.mostImportantTasks(),
        command.team(),
        command.whyNiceJob());
  }
}
