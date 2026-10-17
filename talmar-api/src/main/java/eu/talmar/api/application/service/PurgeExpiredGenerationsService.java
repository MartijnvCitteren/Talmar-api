package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.PurgeExpiredGenerationsUseCase;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
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
public class PurgeExpiredGenerationsService implements PurgeExpiredGenerationsUseCase {
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
