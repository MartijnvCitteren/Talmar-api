package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.identity.valueobject.Password;

/** Turns a password into a slow, salted hash that is safe to store. */
public interface PasswordHasher {

  /**
   * Hashes a password.
   *
   * @return the encoded hash, including the algorithm parameters and salt needed to verify it
   */
  String hash(Password password);
}
