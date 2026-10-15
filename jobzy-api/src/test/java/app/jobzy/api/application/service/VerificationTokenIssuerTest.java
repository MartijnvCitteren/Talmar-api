package app.jobzy.api.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.jobzy.api.domain.identity.valueobject.VerificationToken;
import app.jobzy.api.shared.config.IdentityProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenIssuerTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
  private static final IdentityProperties PROPERTIES =
      new IdentityProperties(URI.create("https://jobzy.app/confirm"), Duration.ofDays(2));

  private final VerificationTokenIssuer issuer = new VerificationTokenIssuer(CLOCK, PROPERTIES);

  @Test
  @DisplayName(
      "given the retention setting, when a token is issued then it stores the hash of the raw"
          + " token and expires after the retention")
  void givenRetentionSettingWhenTokenIsIssuedThenItStoresTheHashAndExpiresAfterRetention() {
    var issued = issuer.issue();

    assertEquals(VerificationToken.hashOf(issued.rawToken()), issued.token().tokenHash());
    assertEquals(LocalDateTime.of(2026, 10, 11, 12, 0), issued.token().expiresAt());
  }

  @Test
  @DisplayName(
      "given two issued tokens, when compared then they differ and are 256-bit URL-safe strings")
  void givenTwoIssuedTokensWhenComparedThenTheyDifferAndAreUrlSafe() {
    var first = issuer.issue().rawToken();
    var second = issuer.issue().rawToken();

    assertNotEquals(first, second);
    assertEquals(43, first.length());
    assertTrue(first.matches("[A-Za-z0-9_-]+"));
  }

  @Test
  @DisplayName("given an issued token, when toString then the raw token is not in the output")
  void givenIssuedTokenWhenToStringThenTheRawTokenIsNotInTheOutput() {
    var issued = issuer.issue();

    assertFalse(issued.toString().contains(issued.rawToken()));
  }
}
