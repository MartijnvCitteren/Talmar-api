package eu.talmar.api.domain.identity.valueobject;

/**
 * A plain-text password, which only lives long enough to be hashed. The password policy (12 to 128
 * characters, no composition rules, following NIST SP 800-63B) is part of the contract and
 * validated by the REST adapter.
 */
public record Password(String value) {

  /** Redacted, so a password never ends up in a log line or exception message. */
  @Override
  public String toString() {
    return "Password[redacted]";
  }
}
