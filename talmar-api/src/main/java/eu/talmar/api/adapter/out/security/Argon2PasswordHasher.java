package eu.talmar.api.adapter.out.security;

import eu.talmar.api.application.port.out.PasswordHasher;
import eu.talmar.api.domain.identity.valueobject.Password;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hashes passwords with Argon2id, the algorithm the OWASP Password Storage Cheat Sheet and RFC 9106
 * recommend first: it is memory-hard, so cracking hashes on GPUs or ASICs costs memory, not just
 * compute. bcrypt would silently ignore everything after 72 bytes of our 128-character passwords.
 *
 * <p>The parameters are RFC 9106's second recommended option, for systems that cannot spend 2 GiB
 * per hash: 64 MiB of memory, 3 iterations, parallelism 4, a 16-byte salt and a 32-byte hash. That
 * is well above the OWASP baseline (19 MiB, 2 iterations). The price is server memory per
 * concurrent login or confirmation, which matters for the container size and makes rate limiting on
 * these endpoints necessary once login exists. The encoded hash carries its parameters and salt, so
 * they can be raised later without breaking existing hashes.
 */
@Component
public class Argon2PasswordHasher implements PasswordHasher {
  private static final int SALT_LENGTH_BYTES = 16;
  private static final int HASH_LENGTH_BYTES = 32;
  private static final int PARALLELISM = 4;
  private static final int MEMORY_KIB = 64 * 1024;
  private static final int ITERATIONS = 3;

  private final Argon2PasswordEncoder encoder =
      new Argon2PasswordEncoder(
          SALT_LENGTH_BYTES, HASH_LENGTH_BYTES, PARALLELISM, MEMORY_KIB, ITERATIONS);

  @Override
  public String hash(Password password) {
    return encoder.encode(password.value());
  }
}
