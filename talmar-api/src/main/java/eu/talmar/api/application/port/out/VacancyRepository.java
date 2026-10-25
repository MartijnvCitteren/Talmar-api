package eu.talmar.api.application.port.out;

import eu.talmar.api.domain.vacancy.Vacancy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public interface VacancyRepository {
  /**
   * Saves a new vacancy, or the changes to a loaded one.
   *
   * @throws VacancyModifiedConcurrentlyException when the vacancy was changed after it was loaded
   */
  void save(Vacancy vacancy);

  Optional<Vacancy> findById(UUID id);
}
