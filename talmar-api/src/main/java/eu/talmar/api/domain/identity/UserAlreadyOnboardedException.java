package eu.talmar.api.domain.identity;

import eu.talmar.api.shared.exception.BaseException;

/** Thrown when an account that has finished onboarding asks to onboard again; nothing changes. */
public class UserAlreadyOnboardedException extends BaseException {

  public UserAlreadyOnboardedException() {
    super("The account has already been onboarded");
  }
}
