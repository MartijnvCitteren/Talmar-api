package eu.talmar.api.application.port.out;

import eu.talmar.api.domain.vacancy.Vacancy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public interface VacancyRepository {
  void save(Vacancy vacancy);

  Optional<Vacancy> findById(UUID id);
}
