package eu.talmar.api.domain.identity.valueobject;

/**
 * Who registers: the profile fields from the registration form, as validated by the REST adapter.
 */
public record Registrant(String firstName, String lastName, EmailAddress email, JobRole jobRole) {

  /** Redacted: names and e-mail address are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "Registrant[jobRole=" + jobRole + "]";
  }
}
