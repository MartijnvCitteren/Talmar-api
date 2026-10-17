package eu.talmar.api.adapter.in.rest.vacancy;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Adapter-level exception thrown by {@code VacancyDescriptionContentValidator} when a request field
 * fails content-safety validation. Lives at the adapter edge, not in the domain, because validation
 * is a request-shape check, not a domain invariant.
 */
public class InvalidVacancyDescriptionRequestException extends BaseException {
  private final String field;

  /**
   * @param field the name of the field that failed validation
   * @param message human-readable validation failure reason
   */
  public InvalidVacancyDescriptionRequestException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
