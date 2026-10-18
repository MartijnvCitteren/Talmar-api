package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.OnboardUserCommand;
import eu.talmar.api.domain.identity.valueobject.UserStatus;

public interface OnboardUserUseCase {

  /**
   * Gives a logged-in account its Talmar user and company, or finishes an earlier onboarding that
   * was interrupted.
   *
   * @return the user's status: {@code ACTIVE} for the founder of a company, {@code
   *     PENDING_APPROVAL} for someone who joined an existing one
   */
  UserStatus onboard(OnboardUserCommand command);
}
