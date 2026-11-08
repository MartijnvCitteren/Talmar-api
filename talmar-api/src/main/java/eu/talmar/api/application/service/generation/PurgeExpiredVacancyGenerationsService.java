package eu.talmar.api.application.service.generation;

import eu.talmar.api.application.port.in.generation.PurgeExpiredVacancyGenerationsUseCase;
import eu.talmar.api.application.port.out.generation.VacancyDescriptionGenerationRepository;
import eu.talmar.api.shared.config.GenerationProperties;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes generations after the retention period. A draft only has to live long enough for the user
 * to pick it up; after that it has no purpose, so it is removed rather than kept "just in case".
 * Touches the generation table only, never a vacancy.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class PurgeExpiredVacancyGenerationsService
    implements PurgeExpiredVacancyGenerationsUseCase {
  private final VacancyDescriptionGenerationRepository generationRepository;
  private final Clock clock;
  private final GenerationProperties generationProperties;

  @Override
  @Transactional
  public void purgeExpired() {
    var cutoff = LocalDateTime.now(clock).minus(generationProperties.retention());
    int deleted = generationRepository.deleteRequestedBefore(cutoff);
    log.info("Purged {} description generations requested before {}", deleted, cutoff);
  }
}
