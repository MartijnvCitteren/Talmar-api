package eu.taldo.api.domain.identity.exception;

import eu.taldo.api.shared.exception.BaseException;

/** Thrown when an account that has finished onboarding asks to onboard again; nothing changes. */
public class UserAlreadyOnboardedException extends BaseException {

  public UserAlreadyOnboardedException() {
    super("The account has already been onboarded");
  }
}
