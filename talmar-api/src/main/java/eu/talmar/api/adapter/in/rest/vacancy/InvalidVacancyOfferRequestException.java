package eu.talmar.api.adapter.in.rest.vacancy;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown by {@code VacancyOfferRequestValidator} when an offer breaks a contract rule the schema
 * cannot express. Lives at the adapter edge because it is a request check, not a domain invariant.
 */
public class InvalidVacancyOfferRequestException extends BaseException {
  private final String field;

  /**
   * @param field the path of the field that failed validation, e.g. {@code offer.currency}
   * @param message human-readable validation failure reason, without the rejected value
   */
  public InvalidVacancyOfferRequestException(String field, String message) {
    super(message);
    this.field = field;
  }

  public String getField() {
    return field;
  }
}
