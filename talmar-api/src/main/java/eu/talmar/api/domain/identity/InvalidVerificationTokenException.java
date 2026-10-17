package eu.talmar.api.domain.identity;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown when a registration is confirmed with a token that is unknown, already used or expired.
 * The cases share one message on purpose: telling them apart would let a caller probe which tokens
 * once existed.
 */
public class InvalidVerificationTokenException extends BaseException {

  public InvalidVerificationTokenException() {
    super("The confirmation link is invalid or has expired");
  }
}
