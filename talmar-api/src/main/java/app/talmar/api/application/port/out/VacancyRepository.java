package app.talmar.api.application.port.out;

import app.talmar.api.domain.vacancy.Vacancy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public interface VacancyRepository {
  void save(Vacancy vacancy);

  Optional<Vacancy> findById(UUID id);
}
