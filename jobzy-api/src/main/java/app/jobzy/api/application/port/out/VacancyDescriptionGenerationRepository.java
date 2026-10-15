package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.generation.VacancyDescriptionGeneration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface VacancyDescriptionGenerationRepository {

  void save(VacancyDescriptionGeneration generation);

  Optional<VacancyDescriptionGeneration> findById(UUID id);

  /**
   * Deletes every generation requested before the given moment.
   *
   * @return the number of deleted generations
   */
  int deleteRequestedBefore(LocalDateTime cutoff);
}
