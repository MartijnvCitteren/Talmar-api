package app.jobzy.api.domain.identity.valueobject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;

/**
 * The e-mail verification token as it is stored: only the SHA-256 hash of the token in the
 * confirmation link, plus its expiry. Storing the hash means a database leak does not hand out
 * working confirmation links. A plain hash without salt is enough here, unlike for passwords,
 * because the token itself is long and random.
 */
public record VerificationToken(String tokenHash, LocalDateTime expiresAt) {

  public VerificationToken {
    Objects.requireNonNull(tokenHash, "tokenHash is required");
    Objects.requireNonNull(expiresAt, "expiresAt is required");
  }

  /** The hex-encoded SHA-256 hash of a raw token, the form in which tokens are stored. */
  public static String hashOf(String rawToken) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is required on every Java platform", e);
    }
  }

  /** Whether the token can no longer be used at the given moment. */
  public boolean isExpiredAt(LocalDateTime moment) {
    return !moment.isBefore(expiresAt);
  }

  /** Compares in constant time, so the comparison does not leak how much of a hash matched. */
  public boolean matches(String candidateHash) {
    return MessageDigest.isEqual(
        tokenHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8));
  }
}
