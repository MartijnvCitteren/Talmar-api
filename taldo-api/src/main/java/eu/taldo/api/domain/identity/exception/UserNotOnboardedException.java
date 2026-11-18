package eu.taldo.api.domain.identity.exception;

import eu.taldo.api.shared.exception.BaseException;

/** Thrown when a logged-in account has no Taldo user yet: it has to onboard first. */
public class UserNotOnboardedException extends BaseException {

  public UserNotOnboardedException() {
    super("The account has not been onboarded yet");
  }
}
