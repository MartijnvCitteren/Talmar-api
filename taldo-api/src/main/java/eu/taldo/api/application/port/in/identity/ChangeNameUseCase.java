package eu.taldo.api.application.port.in.identity;

import eu.taldo.api.application.port.in.identity.command.ChangeNameCommand;

public interface ChangeNameUseCase {

  /**
   * Changes the logged-in user's first and/or last name, at the identity provider and in Taldo's
   * copy. Repeating it with the same names changes nothing further.
   */
  void changeName(ChangeNameCommand command);
}
