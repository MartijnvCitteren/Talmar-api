package eu.talmar.api.domain.identity;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown when an account may not create or join a company: its e-mail address is not verified, or
 * it is on a public mail domain. Nothing is saved.
 */
public class OnboardingNotAllowedException extends BaseException {

  /**
   * @param reason why, without the address itself
   */
  public OnboardingNotAllowedException(String reason) {
    super("The account may not onboard: " + reason);
  }
}
