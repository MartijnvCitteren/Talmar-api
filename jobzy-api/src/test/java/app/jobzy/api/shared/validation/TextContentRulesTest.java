package app.jobzy.api.shared.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextContentRulesTest {

  @ParameterizedTest
  @ValueSource(strings = {"<script>", "</div>", "<img src=x", "<!-- comment -->", "a <b>bold</b>"})
  @DisplayName("given text with a raw tag, when checked then unsafe")
  void givenTextWithRawTagWhenCheckedThenUnsafe(String text) {
    assertTrue(TextContentRules.containsRawTag(text));
    assertFalse(TextContentRules.isSafe(text));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\u0000", "\u0007", "\u001B", "\u007F", "\u009F"})
  @DisplayName("given text with a disallowed control character, when checked then unsafe")
  void givenTextWithControlCharacterWhenCheckedThenUnsafe(String character) {
    var text = "Before" + character + "after";

    assertTrue(TextContentRules.containsDisallowedControlCharacter(text));
    assertFalse(TextContentRules.isSafe(text));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"Plain text", "Line one\nLine two\r\n\tIndented", "1 < 2 and 3 > 2", "- a task"})
  @DisplayName("given ordinary text, when checked then safe")
  void givenOrdinaryTextWhenCheckedThenSafe(String text) {
    assertTrue(TextContentRules.isSafe(text));
  }
}
