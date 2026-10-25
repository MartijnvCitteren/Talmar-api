package eu.talmar.api.application.port.out;

import eu.talmar.api.shared.exception.BaseException;
import java.util.UUID;

/**
 * Thrown by {@link VacancyRepository#save} when the vacancy was changed by someone else after it
 * was loaded. Saving anyway would silently undo their change, so the user has to reload and try
 * again.
 */
public class VacancyModifiedConcurrentlyException extends BaseException {

  /**
   * @param id the id of the vacancy that was changed in the meantime
   */
  public VacancyModifiedConcurrentlyException(UUID id) {
    super("Vacancy was modified concurrently: " + id);
  }
}
