package app.jobzy.api.domain.identity.valueobject;

import java.util.Objects;

/**
 * How a user proves who they are; for now only a password hash (Argon2id, encoded with its
 * parameters and salt). Kept apart from the user's profile so that an external identity (SSO) can
 * sit next to or replace the password later without touching the user itself.
 */
public record UserCredentials(String passwordHash) {

  public UserCredentials {
    Objects.requireNonNull(passwordHash, "passwordHash is required");
  }

  /** Redacted: a password hash must not end up in logs. */
  @Override
  public String toString() {
    return "UserCredentials[redacted]";
  }
}
