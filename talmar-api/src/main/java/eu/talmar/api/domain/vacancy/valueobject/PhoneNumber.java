package eu.talmar.api.domain.vacancy.valueobject;

/**
 * A phone number in one canonical form: digits only, with a leading {@code +} for an international
 * number. People type the same number in many ways ({@code +31 (0)6 12 34 56 78}, {@code 0031
 * 6-12345678}, {@code +31612345678}); stored and shown the same way, it reads consistently and can
 * be compared.
 *
 * <p>Two European conventions are rewritten before the separators are dropped. A leading {@code 00}
 * is the international call prefix, so it becomes {@code +}. In an international number, a {@code
 * (0)} marks the trunk prefix that is only dialled from inside the country, so it is removed;
 * keeping its zero would make the number undiallable. A national number stays national: the country
 * code cannot be derived reliably.
 *
 * <p>The shape (6 to 15 digits, separators only) is validated by the REST adapter against the
 * contract before it gets here. Normalising an already normalised number returns it unchanged.
 */
public record PhoneNumber(String value) {
  private static final String INTERNATIONAL_PLUS = "+";
  private static final String INTERNATIONAL_CALL_PREFIX = "00";
  private static final String TRUNK_PREFIX = "(0)";

  public PhoneNumber {
    value = normalise(value);
  }

  private static String normalise(String phoneNumber) {
    var stripped = phoneNumber.strip();
    if (!isInternational(stripped)) {
      return digitsOnly(stripped);
    }
    var digits = digitsOnly(stripped.replace(TRUNK_PREFIX, ""));
    var countryCodeAndNumber =
        stripped.startsWith(INTERNATIONAL_CALL_PREFIX)
            ? digits.substring(INTERNATIONAL_CALL_PREFIX.length())
            : digits;
    return INTERNATIONAL_PLUS + countryCodeAndNumber;
  }

  private static boolean isInternational(String phoneNumber) {
    return phoneNumber.startsWith(INTERNATIONAL_PLUS)
        || phoneNumber.startsWith(INTERNATIONAL_CALL_PREFIX);
  }

  private static String digitsOnly(String phoneNumber) {
    return phoneNumber.replaceAll("\\D", "");
  }

  /** Redacted: a phone number is personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "PhoneNumber[redacted]";
  }
}
