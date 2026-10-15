package app.jobzy.api.application.port.in;

import app.jobzy.api.application.port.in.command.ConfirmRegistrationCommand;
import app.jobzy.api.domain.identity.valueobject.UserStatus;

public interface ConfirmRegistrationUseCase {

  /**
   * Confirms a registration and sets the password.
   *
   * @return the user's status afterwards: {@code ACTIVE} or {@code PENDING_APPROVAL}
   */
  UserStatus confirm(ConfirmRegistrationCommand command);
}
