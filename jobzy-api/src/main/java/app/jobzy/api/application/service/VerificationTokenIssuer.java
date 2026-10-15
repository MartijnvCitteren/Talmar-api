package app.jobzy.api.application.service;

import app.jobzy.api.domain.identity.valueobject.VerificationToken;
import app.jobzy.api.shared.config.IdentityProperties;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Issues e-mail verification tokens: 256 random bits from {@link SecureRandom}, URL-safe Base64
 * encoded for the confirmation link. The raw token goes into the mail only; the user keeps its
 * SHA-256 hash. A token is valid for {@code jobzy.identity.unconfirmed-retention}, the same period
 * after which the purge deletes the unconfirmed registration.
 */
@Component
@RequiredArgsConstructor
public class VerificationTokenIssuer {
  private static final int TOKEN_BYTES = 32;

  private final SecureRandom secureRandom = new SecureRandom();
  private final Clock clock;
  private final IdentityProperties identityProperties;

  public IssuedVerificationToken issue() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    LocalDateTime expiresAt =
        LocalDateTime.now(clock).plus(identityProperties.unconfirmedRetention());
    return new IssuedVerificationToken(
        rawToken, new VerificationToken(VerificationToken.hashOf(rawToken), expiresAt));
  }

  /**
   * A freshly issued token in both forms.
   *
   * @param rawToken the token for the confirmation link
   * @param token the stored form: hash and expiry
   */
  public record IssuedVerificationToken(String rawToken, VerificationToken token) {

    /** Redacted: the raw token is a secret. */
    @Override
    public String toString() {
      return "IssuedVerificationToken[redacted]";
    }
  }
}
