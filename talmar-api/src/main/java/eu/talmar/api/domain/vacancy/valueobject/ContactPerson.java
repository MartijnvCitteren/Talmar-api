package eu.talmar.api.domain.vacancy.valueobject;

import eu.talmar.api.domain.identity.valueobject.EmailAddress;

/**
 * The person candidates can contact about a vacancy. Free text, not a Talmar user: it is often a
 * hiring manager without an account.
 *
 * <p>Name, phone number and e-mail address identify a real person, so they are personal data; the
 * role describes the job and is not. {@code role} and {@code phone} are optional.
 */
public record ContactPerson(String name, String role, PhoneNumber phone, EmailAddress email) {

  /** Redacted: name, phone number and e-mail address are personal data. */
  @Override
  public String toString() {
    return "ContactPerson[role=" + role + ", name, phone and email redacted]";
  }
}
