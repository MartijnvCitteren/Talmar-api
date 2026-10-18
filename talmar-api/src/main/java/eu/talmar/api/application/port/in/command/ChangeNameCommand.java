package eu.talmar.api.application.port.in.command;

import org.jspecify.annotations.Nullable;

/**
 * Changes the logged-in user's names. A name that is {@code null} stays as it is.
 *
 * @param subject the logged-in account's id at the identity provider, from its access token
 */
public record ChangeNameCommand(
    String subject, @Nullable String firstName, @Nullable String lastName) {

  /** Whether the command changes anything at all. */
  public boolean isEmpty() {
    return firstName == null && lastName == null;
  }

  /** Redacted: names are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "ChangeNameCommand[subject=" + subject + "]";
  }
}
