package eu.talmar.api.adapter.in.rest.generation;

import eu.talmar.api.shared.exception.BaseException;
import lombok.Getter;

/**
 * Thrown by {@code GenerateVacancyDescriptionRequestValidator} when an answer fails the content
 * rules. Adapter-level: it is a request-shape check, not a domain invariant.
 */
@Getter
public class InvalidGenerationRequestException extends BaseException {
  private final String field;

  /**
   * @param field the name of the field that failed validation
   * @param message human-readable validation failure reason
   */
  public InvalidGenerationRequestException(String field, String message) {
    super(message);
    this.field = field;
  }
}
