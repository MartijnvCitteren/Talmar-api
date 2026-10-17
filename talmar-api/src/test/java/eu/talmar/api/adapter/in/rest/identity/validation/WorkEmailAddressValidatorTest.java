package eu.talmar.api.adapter.in.rest.identity.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.talmar.api.adapter.in.rest.identity.PublicEmailDomainException;
import eu.talmar.api.identity.adapter.in.web.contract.RegistrationRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;

class WorkEmailAddressValidatorTest {

  private final WorkEmailAddressValidator shippedList = new WorkEmailAddressValidator();

  @ParameterizedTest
  @ValueSource(
      strings = {
        "gmail.com",
        "googlemail.com",
        "outlook.com",
        "hotmail.com",
        "hotmail.co.uk",
        "live.nl",
        "yahoo.fr",
        "icloud.com",
        "proton.me",
        "protonmail.com",
        "gmx.de",
        "ziggo.nl",
        "kpnmail.nl",
        "GMAIL.COM"
      })
  @DisplayName("given a public mail domain, when validated against the shipped list then rejected")
  void givenPublicMailDomainWhenValidatedAgainstShippedListThenRejected(String domain) {
    var request = registration("jane@" + domain);

    assertThrows(PublicEmailDomainException.class, () -> shippedList.validate(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"acme.nl", "talmar.eu", "sub.gmail.com.acme.nl", "mygmail.com"})
  @DisplayName("given a company domain, when validated against the shipped list then accepted")
  void givenCompanyDomainWhenValidatedAgainstShippedListThenAccepted(String domain) {
    var request = registration("jane@" + domain);

    assertDoesNotThrow(() -> shippedList.validate(request));
  }

  @Test
  @DisplayName(
      "given a list with comments, blank lines and upper case, when read then only the entries"
          + " count")
  void givenListWithCommentsBlankLinesAndUpperCaseWhenReadThenOnlyTheEntriesCount() {
    var validator =
        new WorkEmailAddressValidator(
            new ByteArrayResource(
                "# comment\n\n  Example.ORG  # trailing comment\nfree.*\n"
                    .getBytes(StandardCharsets.UTF_8)));

    assertThrows(
        PublicEmailDomainException.class,
        () -> validator.validate(registration("jane@example.org")));
    assertThrows(
        PublicEmailDomainException.class, () -> validator.validate(registration("jane@free.fr")));
    assertDoesNotThrow(() -> validator.validate(registration("jane@comment")));
    assertDoesNotThrow(() -> validator.validate(registration("jane@freedom.fr")));
  }

  private static RegistrationRequest registration(String email) {
    return new RegistrationRequest().email(email);
  }
}
