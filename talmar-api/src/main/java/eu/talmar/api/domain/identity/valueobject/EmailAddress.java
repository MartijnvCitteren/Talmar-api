package eu.talmar.api.domain.identity.valueobject;

import java.util.Locale;

/**
 * An e-mail address, normalised to lower case so that the same mailbox always maps to the same user
 * and the same domain to the same company.
 *
 * <p>Its shape (one {@code @}, a domain with a dot, no whitespace, at most 75 characters) is
 * validated by the REST adapter against the contract before it gets here, so {@link #domain()} can
 * rely on it.
 */
public record EmailAddress(String value) {

  public EmailAddress {
    value = value.toLowerCase(Locale.ROOT);
  }

  /** The part after the {@code @}, in lower case. The key that links a user to a company. */
  public String domain() {
    return value.substring(value.indexOf('@') + 1);
  }

  /** Redacted: an e-mail address is personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "EmailAddress[redacted]";
  }
}
