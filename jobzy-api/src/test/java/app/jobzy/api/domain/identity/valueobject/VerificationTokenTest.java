package app.jobzy.api.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VerificationTokenTest {
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.of(2026, 10, 11, 12, 0);

  @Test
  @DisplayName("given a raw token, when hashed then returns its hex-encoded SHA-256")
  void givenRawTokenWhenHashedThenReturnsItsHexEncodedSha256() {
    assertEquals(
        "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824",
        VerificationToken.hashOf("hello"));
  }

  @Test
  @DisplayName("given two different raw tokens, when hashed then the hashes differ")
  void givenTwoDifferentRawTokensWhenHashedThenTheHashesDiffer() {
    assertNotEquals(VerificationToken.hashOf("token-a"), VerificationToken.hashOf("token-b"));
  }

  @Test
  @DisplayName("given a moment before the expiry, when isExpiredAt then returns false")
  void givenMomentBeforeExpiryWhenIsExpiredAtThenReturnsFalse() {
    var token = new VerificationToken("hash", EXPIRES_AT);

    assertFalse(token.isExpiredAt(EXPIRES_AT.minusSeconds(1)));
  }

  @Test
  @DisplayName("given the exact expiry moment, when isExpiredAt then returns true")
  void givenExactExpiryMomentWhenIsExpiredAtThenReturnsTrue() {
    var token = new VerificationToken("hash", EXPIRES_AT);

    assertTrue(token.isExpiredAt(EXPIRES_AT));
  }

  @Test
  @DisplayName("given the stored hash, when matches then returns true, and false for another hash")
  void givenStoredHashWhenMatchesThenReturnsTrueAndFalseForAnotherHash() {
    var token = new VerificationToken("hash", EXPIRES_AT);

    assertTrue(token.matches("hash"));
    assertFalse(token.matches("other"));
  }
}
