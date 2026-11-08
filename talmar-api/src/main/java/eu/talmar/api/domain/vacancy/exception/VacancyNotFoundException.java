package eu.talmar.api.domain.vacancy.exception;

import eu.talmar.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown when a vacancy lookup by id finds no match. Used by use cases that require an existing
 * vacancy to exist; mapped to 404 Problem Details by {@code VacancyExceptionHandler}.
 */
public class VacancyNotFoundException extends BaseException {

  /**
   * @param id the vacancy id that was not found
   */
  public VacancyNotFoundException(UUID id) {
    super("Vacancy not found: " + id);
  }
}
