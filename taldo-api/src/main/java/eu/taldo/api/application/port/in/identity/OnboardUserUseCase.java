package eu.taldo.api.application.port.in.identity;

import eu.taldo.api.application.port.in.identity.command.OnboardUserCommand;
import eu.taldo.api.domain.identity.valueobject.UserStatus;

public interface OnboardUserUseCase {

  /**
   * Gives a logged-in account its Taldo user and company, or finishes an earlier onboarding that
   * was interrupted.
   *
   * @return the user's status: {@code ACTIVE} for the founder of a company, {@code
   *     PENDING_APPROVAL} for someone who joined an existing one
   */
  UserStatus onboard(OnboardUserCommand command);
}
