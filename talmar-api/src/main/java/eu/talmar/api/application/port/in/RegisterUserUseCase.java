package eu.talmar.api.application.port.in;

import eu.talmar.api.application.port.in.command.RegisterUserCommand;

public interface RegisterUserUseCase {

  /**
   * Registers a user and sends a mail. Deliberately returns nothing: the caller must not be able to
   * tell a new registration from an existing one (enumeration).
   */
  void register(RegisterUserCommand command);
}
