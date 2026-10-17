package eu.talmar.api.domain.generation;

import eu.talmar.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown when a polled generation failed because the AI provider refused the request in a way a
 * retry does not fix. AI generation is unavailable until we fix it.
 */
public class VacancyDescriptionGenerationUnavailableException extends BaseException {

  /**
   * @param generationId the generation the provider refused
   */
  public VacancyDescriptionGenerationUnavailableException(UUID generationId) {
    super("Description generation " + generationId + " failed: the AI provider is unavailable");
  }
}
