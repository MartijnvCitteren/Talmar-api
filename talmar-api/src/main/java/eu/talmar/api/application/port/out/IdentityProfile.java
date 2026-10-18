package eu.talmar.api.application.port.out;

import eu.talmar.api.domain.identity.valueobject.EmailAddress;

/**
 * An account's profile at the identity provider, validated by the adapter against the same rules as
 * Talmar's own columns.
 *
 * @param emailVerified whether the provider has verified that the account owns the e-mail address
 */
public record IdentityProfile(
    String firstName, String lastName, EmailAddress email, boolean emailVerified) {

  /** Redacted: names and e-mail address are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "IdentityProfile[emailVerified=" + emailVerified + "]";
  }
}
