package eu.talmar.api.adapter.in.rest.identity;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown by {@code WorkEmailAddressValidator} when a registration uses an address on a public mail
 * domain: such an address says nothing about the company someone works for. The message never
 * contains the address itself, because it is personal data and ends up in responses and logs.
 */
public class PublicEmailDomainException extends BaseException {

  public PublicEmailDomainException() {
    super("Use your work e-mail address; public mail domains are not accepted");
  }
}
