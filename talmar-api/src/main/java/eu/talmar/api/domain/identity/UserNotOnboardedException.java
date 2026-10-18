package eu.talmar.api.domain.identity;

import eu.talmar.api.shared.exception.BaseException;

/** Thrown when a logged-in account has no Talmar user yet: it has to onboard first. */
public class UserNotOnboardedException extends BaseException {

  public UserNotOnboardedException() {
    super("The account has not been onboarded yet");
  }
}
