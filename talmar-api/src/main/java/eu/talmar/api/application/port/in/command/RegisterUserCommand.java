package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import lombok.Builder;

@Builder
public record RegisterUserCommand(
    String firstName, String lastName, EmailAddress email, JobRole jobRole) {

  /** Redacted: names and e-mail address are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "RegisterUserCommand[jobRole=" + jobRole + "]";
  }
}
