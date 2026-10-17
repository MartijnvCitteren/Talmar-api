package app.talmar.api.application.port.in;

import app.talmar.api.application.port.in.command.ConfirmRegistrationCommand;
import app.talmar.api.domain.identity.valueobject.UserStatus;

public interface ConfirmRegistrationUseCase {

  /**
   * Confirms a registration and sets the password.
   *
   * @return the user's status afterwards: {@code ACTIVE} or {@code PENDING_APPROVAL}
   */
  UserStatus confirm(ConfirmRegistrationCommand command);
}
