package app.jobzy.api.domain.identity;

import app.jobzy.api.shared.exception.BaseException;

/**
 * Thrown when an e-mail address cannot be used to register because it is on a public mail domain:
 * such an address says nothing about the company someone works for. A malformed address never gets
 * this far; the REST adapter rejects it against the contract. The message never contains the
 * address itself, because it is personal data and ends up in responses and logs.
 */
public class InvalidEmailAddressException extends BaseException {

  /**
   * @param message why the address cannot be used, without the address itself
   */
  public InvalidEmailAddressException(String message) {
    super(message);
  }
}
