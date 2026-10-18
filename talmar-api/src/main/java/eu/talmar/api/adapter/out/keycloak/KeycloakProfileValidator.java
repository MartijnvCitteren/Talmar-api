package eu.talmar.api.adapter.out.keycloak;

import java.util.Arrays;

/**
 * Checks a profile from Keycloak against the rules of Talmar's own columns before it reaches the
 * application: names of 1 to 35 characters from the same whitelist as the realm's user profile, and
 * an e-mail address of at most 75 characters with a domain that contains a dot. The realm enforces
 * the same rules (ADR 0006, section 8); this guards our columns if its configuration ever changes.
 *
 * <p>Checked character by character instead of with the realm's regular expressions: those have
 * nested quantifiers, which backtrack badly on hostile input (ReDoS).
 */
final class KeycloakProfileValidator {
  private static final int MAX_NAME_LENGTH = 35;
  private static final int MAX_EMAIL_LENGTH = 75;
  private static final String NAME_PUNCTUATION = "'’.-";

  private KeycloakProfileValidator() {}

  /**
   * @throws InvalidKeycloakProfileException naming the first field that breaks a rule
   */
  static void validate(KeycloakUser user) {
    validateName("firstName", user.firstName());
    validateName("lastName", user.lastName());
    validateEmail(user.email());
  }

  private static void validateName(String field, String name) {
    if (name == null || name.length() > MAX_NAME_LENGTH || !isPersonName(name)) {
      throw new InvalidKeycloakProfileException(field);
    }
  }

  /**
   * Letters of any script with their combining marks, apostrophes, periods and hyphens, single
   * spaces between words, and at least one letter.
   */
  private static boolean isPersonName(String name) {
    return name.codePoints().anyMatch(Character::isLetter)
        && name.codePoints().allMatch(KeycloakProfileValidator::isNameCharacter)
        && !name.startsWith(" ")
        && !name.endsWith(" ")
        && !name.contains("  ");
  }

  private static boolean isNameCharacter(int codePoint) {
    return Character.isLetter(codePoint)
        || isCombiningMark(codePoint)
        || codePoint == ' '
        || NAME_PUNCTUATION.indexOf(codePoint) >= 0;
  }

  private static boolean isCombiningMark(int codePoint) {
    int type = Character.getType(codePoint);
    return type == Character.NON_SPACING_MARK
        || type == Character.COMBINING_SPACING_MARK
        || type == Character.ENCLOSING_MARK;
  }

  private static void validateEmail(String email) {
    if (email == null || email.length() > MAX_EMAIL_LENGTH || !isEmailAddress(email)) {
      throw new InvalidKeycloakProfileException("email");
    }
  }

  /** One {@code @}, no whitespace, a local part, and a domain of at least two non-empty labels. */
  private static boolean isEmailAddress(String email) {
    int at = email.indexOf('@');
    if (at <= 0
        || at != email.lastIndexOf('@')
        || email.chars().anyMatch(Character::isWhitespace)) {
      return false;
    }
    var labels = email.substring(at + 1).split("\\.", -1);
    return labels.length >= 2 && Arrays.stream(labels).noneMatch(String::isEmpty);
  }
}
