package eu.talmar.api.domain.generation;

import eu.talmar.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown when a generation does not exist for the given vacancy: never started, started for another
 * vacancy, or already deleted by the retention clean-up.
 */
public class VacancyDescriptionGenerationNotFoundException extends BaseException {

  /**
   * @param vacancyId the vacancy the generation was looked up for
   * @param generationId the generation id that was not found
   */
  public VacancyDescriptionGenerationNotFoundException(UUID vacancyId, UUID generationId) {
    super("Description generation " + generationId + " not found for vacancy " + vacancyId);
  }

  /**
   * @param generationId the generation id that was not found
   */
  public VacancyDescriptionGenerationNotFoundException(UUID generationId) {
    super("Description generation " + generationId + " not found");
  }
}
