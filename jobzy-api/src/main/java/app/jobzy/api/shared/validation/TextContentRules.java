package app.jobzy.api.shared.validation;

import java.util.regex.Pattern;

/**
 * Content rules for free text that ends up in a vacancy: no raw HTML or script-like tags, and no
 * control characters other than newline, carriage return and tab. Shared by the inbound request
 * validators and by the check on AI-generated drafts, so a generated draft is held to exactly the
 * same rules as text a user types.
 */
public final class TextContentRules {

  private static final Pattern RAW_TAG = Pattern.compile("</?[a-zA-Z!][^>]*>?");
  private static final Pattern DISALLOWED_CONTROL_CHARACTER =
      Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F]");

  private TextContentRules() {}

  /** Whether the text contains something that looks like an HTML or script tag. */
  public static boolean containsRawTag(String text) {
    return RAW_TAG.matcher(text).find();
  }

  /** Whether the text contains a control character other than newline, carriage return or tab. */
  public static boolean containsDisallowedControlCharacter(String text) {
    return DISALLOWED_CONTROL_CHARACTER.matcher(text).find();
  }

  /** Whether the text passes both rules. */
  public static boolean isSafe(String text) {
    return !containsRawTag(text) && !containsDisallowedControlCharacter(text);
  }
}
