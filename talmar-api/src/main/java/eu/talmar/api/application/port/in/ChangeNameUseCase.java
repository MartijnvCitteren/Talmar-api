package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.ChangeNameCommand;

public interface ChangeNameUseCase {

  /**
   * Changes the logged-in user's first and/or last name, at the identity provider and in Talmar's
   * copy. Repeating it with the same names changes nothing further.
   */
  void changeName(ChangeNameCommand command);
}
