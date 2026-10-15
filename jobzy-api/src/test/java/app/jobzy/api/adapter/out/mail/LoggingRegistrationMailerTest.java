package app.jobzy.api.adapter.out.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.jobzy.api.shared.config.IdentityProperties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LoggingRegistrationMailerTest {

  @Test
  @DisplayName(
      "given a confirmation url, when the link is built then the token is appended as query"
          + " parameter")
  void givenConfirmationUrlWhenLinkIsBuiltThenTokenIsAppendedAsQueryParameter() {
    var mailer =
        new LoggingRegistrationMailer(
            new IdentityProperties(
                URI.create("https://jobzy.app/registration/confirm"), Duration.ofDays(2)));

    var link = mailer.confirmationLink("abc_DEF-123");

    assertEquals(URI.create("https://jobzy.app/registration/confirm?token=abc_DEF-123"), link);
  }
}
