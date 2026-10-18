package eu.talmar.api.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.domain.identity.valueobject.Password;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

class Argon2PasswordHasherTest {
  private static final Password PASSWORD = new Password("correct horse battery staple");

  private final Argon2PasswordHasher hasher = new Argon2PasswordHasher();

  @Test
  @DisplayName(
      "given a password, when hashed then the result is an Argon2id hash with the RFC 9106"
          + " parameters that verifies the password")
  void givenPasswordWhenHashedThenResultIsVerifiableArgon2idHashWithRfc9106Parameters() {
    var hash = hasher.hash(PASSWORD);

    assertTrue(hash.startsWith("$argon2id$v=19$m=65536,t=3,p=4$"));
    assertTrue(
        Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().matches(PASSWORD.value(), hash));
  }

  @Test
  @DisplayName("given the same password twice, when hashed then the hashes differ by their salt")
  void givenSamePasswordTwiceWhenHashedThenTheHashesDifferByTheirSalt() {
    assertNotEquals(hasher.hash(PASSWORD), hasher.hash(PASSWORD));
  }
}
