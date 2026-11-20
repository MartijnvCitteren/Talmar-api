package eu.taldo.api.domain.vacancy.generation.exception;

import eu.taldo.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown when a polled generation failed because the model rejected the user's answers as not
 * describing a job. Only changing the answers helps.
 */
public class VacancyDescriptionGenerationRejectedException extends BaseException {

  /**
   * @param generationId the generation whose input was rejected
   */
  public VacancyDescriptionGenerationRejectedException(UUID generationId) {
    super("Description generation " + generationId + " failed: the input was rejected");
  }
}
