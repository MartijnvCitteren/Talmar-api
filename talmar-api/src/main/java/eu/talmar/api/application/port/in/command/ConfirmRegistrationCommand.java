package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.identity.valueobject.Password;
import lombok.Builder;

@Builder
public record ConfirmRegistrationCommand(String token, Password password) {

  /** Redacted: both the token and the password are secrets. */
  @Override
  public String toString() {
    return "ConfirmRegistrationCommand[redacted]";
  }
}
