package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.UpdateVacancyUseCase;
import eu.talmar.api.application.port.in.command.UpdateVacancyCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import java.util.UUID;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes the parts of a vacancy the user sent and leaves the rest as it is. A part that is sent
 * replaces the current one as a whole, so a field the user cleared inside it is cleared here too.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class UpdateVacancyService implements UpdateVacancyUseCase {
  private final VacancyRepository vacancyRepository;

  @Override
  @Transactional
  public Vacancy updateVacancy(UpdateVacancyCommand command) {
    Vacancy vacancy = getVacancy(command.vacancyId());

    applyChanges(command, vacancy);

    log.info("Vacancy is updated: {}", vacancy.getId());
    vacancyRepository.save(vacancy);

    return vacancy;
  }

  private Vacancy getVacancy(UUID vacancyId) {
    return vacancyRepository
        .findById(vacancyId)
        .orElseThrow(() -> new VacancyNotFoundException(vacancyId));
  }

  private static void applyChanges(UpdateVacancyCommand command, Vacancy vacancy) {
    applyIfGiven(command.jobTitle(), vacancy::setJobTitle);
    applyIfGiven(command.language(), vacancy::setLanguage);
    applyIfGiven(command.category(), vacancy::setCategory);
    applyIfGiven(command.location(), vacancy::setLocation);
    applyIfGiven(command.workplaceType(), vacancy::setWorkplaceType);
    applyIfGiven(command.hoursPerWeek(), vacancy::setHoursPerWeek);
    applyIfGiven(command.description(), vacancy::setDescription);
    applyIfGiven(command.contactPerson(), vacancy::setContactPerson);
    applyIfGiven(command.offer(), vacancy::setOffer);
  }

  private static <T> void applyIfGiven(@Nullable T value, Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }
}
