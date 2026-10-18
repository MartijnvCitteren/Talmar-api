package eu.talmar.api.application.port.out;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown by an {@link IdentityProvider} when the provider cannot be reached or answers in a way we
 * did not expect. The adapter has logged the cause; the message names the operation only.
 */
public class IdentityProviderUnavailableException extends BaseException {

  /**
   * @param operation what the call tried to do, e.g. "get profile"
   */
  public IdentityProviderUnavailableException(String operation) {
    super("Identity provider call failed: " + operation);
  }
}
