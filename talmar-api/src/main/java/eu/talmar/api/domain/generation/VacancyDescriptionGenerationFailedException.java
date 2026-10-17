package eu.talmar.api.domain.generation;

import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown when a polled generation failed on our side or timed out. Starting a new generation may
 * help. The reason is only in the message, for the log; the user never sees it.
 */
public class VacancyDescriptionGenerationFailedException extends BaseException {

  /**
   * @param generationId the generation that failed
   * @param reason why it failed
   */
  public VacancyDescriptionGenerationFailedException(
      UUID generationId, GenerationFailureReason reason) {
    super("Description generation " + generationId + " failed: " + reason);
  }
}
