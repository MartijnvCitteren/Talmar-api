package eu.talmar.api.domain.vacancy.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneNumberTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "+31612345678           | +31612345678",
        "+31 6 12 34 56 78      | +31612345678",
        "+31 (0)6 12 34 56 78   | +31612345678",
        "+31 (0)6-1234 5678     | +31612345678",
        "0031 6 12345678        | +31612345678",
        "0031 (0)6 12345678     | +31612345678",
        "+49 (0)30 123456       | +4930123456",
        "+32.2.123.45.67        | +3221234567",
        "06-12345678            | 0612345678",
        "(020) 123 45 67        | 0201234567",
        "030/123456             | 030123456"
      })
  @DisplayName(
      "given a phone number with separators, when created then it is digits only with a leading"
          + " plus for an international number")
  void givenPhoneNumberWithSeparatorsWhenCreatedThenItIsNormalised(
      String typed, String normalised) {
    var phoneNumber = new PhoneNumber(typed);

    assertEquals(normalised, phoneNumber.value());
  }

  @Test
  @DisplayName("given a normalised phone number, when created again then it is unchanged")
  void givenNormalisedPhoneNumberWhenCreatedAgainThenItIsUnchanged() {
    var normalised = new PhoneNumber("0031 (0)6 12 34 56 78").value();

    assertEquals(normalised, new PhoneNumber(normalised).value());
  }

  @Test
  @DisplayName("given a phone number, when toString then the number is not in the output")
  void givenPhoneNumberWhenToStringThenTheNumberIsNotInTheOutput() {
    var phoneNumber = new PhoneNumber("+31612345678");

    assertFalse(phoneNumber.toString().contains("612345678"));
  }
}
