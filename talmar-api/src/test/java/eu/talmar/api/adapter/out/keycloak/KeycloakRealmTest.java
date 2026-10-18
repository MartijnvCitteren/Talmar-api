package eu.talmar.api.adapter.out.keycloak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guards the rule that no public mail address gets a Keycloak account (ADR 0006, section 3). The
 * realm enforces it with a pattern on the e-mail attribute of its user profile, which Keycloak
 * checks on registration, in the account console and in the Admin API. {@code
 * public-email-domains.txt} stays the only list: when it changes, this test fails and prints the
 * pattern to paste into the realm file.
 */
class KeycloakRealmTest {

  private static final Path REALM_FILE = Path.of("../docker/keycloak/talmar-realm.json");
  private static final String USER_PROFILE_PROVIDER =
      "org.keycloak.userprofile.UserProfileProvider";
  private static final String DOMAIN_LIST = "identity/public-email-domains.txt";
  private static final String ANY_TOP_LEVEL_DOMAIN = ".*";
  private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

  @Test
  @DisplayName(
      "given the public mail domain list, when compared with the realm, then the e-mail pattern"
          + " matches the list")
  void givenThePublicMailDomainListWhenComparedWithTheRealmThenTheEmailPatternMatchesTheList()
      throws IOException {
    var expectedPattern = emailPatternFrom(publicMailDomains());

    assertEquals(
        expectedPattern,
        realmEmailPattern().pattern(),
        "The e-mail pattern in " + REALM_FILE + " is out of date with " + DOMAIN_LIST);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "jan@gmail.com",
        "jan@GMAIL.COM",
        "jan@hotmail.co.uk",
        "jan@live.nl",
        "jan@yahoo.fr",
        "jan@t-online.de",
        "jan@163.com"
      })
  @DisplayName("given a public mail address, when checked by the realm, then rejected")
  void givenAPublicMailAddressWhenCheckedByTheRealmThenRejected(String address) throws IOException {
    var realmEmailPattern = realmEmailPattern();

    assertFalse(realmEmailPattern.matcher(address).matches());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "jan@acme.nl",
        "jan@mygmail.com",
        "jan@gmail.com.acme.nl",
        "jan@hotmailer.com",
        "jan@livenation.com"
      })
  @DisplayName("given a work address, when checked by the realm, then accepted")
  void givenAWorkAddressWhenCheckedByTheRealmThenAccepted(String address) throws IOException {
    var realmEmailPattern = realmEmailPattern();

    assertTrue(realmEmailPattern.matcher(address).matches());
  }

  /**
   * The pattern an address must match to be accepted: an exact domain or, for an entry ending in
   * {@code .*}, the name under any top-level domain. {@code [.]} instead of {@code \.} keeps the
   * pattern free of backslashes, so it can be pasted into the JSON string as it is.
   */
  private static String emailPatternFrom(List<String> entries) {
    var exactDomains =
        entries.stream()
            .filter(entry -> !entry.endsWith(ANY_TOP_LEVEL_DOMAIN))
            .map(KeycloakRealmTest::literal)
            .collect(Collectors.joining("|"));
    var namesUnderAnyTopLevelDomain =
        entries.stream()
            .filter(entry -> entry.endsWith(ANY_TOP_LEVEL_DOMAIN))
            .map(entry -> literal(entry.substring(0, entry.length() - 2)))
            .collect(Collectors.joining("|"));
    return "(?i)^[^@]+@(?!(?:"
        + exactDomains
        + ")$)(?!(?:"
        + namesUnderAnyTopLevelDomain
        + ")[.]).+$";
  }

  private static String literal(String domain) {
    return domain.replace(".", "[.]");
  }

  private static List<String> publicMailDomains() throws IOException {
    return new ClassPathResource(DOMAIN_LIST)
        .getContentAsString(StandardCharsets.UTF_8)
        .lines()
        .map(line -> line.replaceFirst("#.*", "").trim().toLowerCase(Locale.ROOT))
        .filter(line -> !line.isEmpty())
        .toList();
  }

  /** The user profile is stored in the realm as a JSON document inside a string. */
  private static Pattern realmEmailPattern() throws IOException {
    var realm = JSON_MAPPER.readTree(Files.readString(REALM_FILE));
    var userProfile =
        JSON_MAPPER.readTree(
            realm
                .path("components")
                .path(USER_PROFILE_PROVIDER)
                .path(0)
                .path("config")
                .path("kc.user.profile.config")
                .path(0)
                .asString());
    for (JsonNode attribute : userProfile.path("attributes")) {
      if ("email".equals(attribute.path("name").asString())) {
        return Pattern.compile(
            attribute.path("validations").path("pattern").path("pattern").asString());
      }
    }
    throw new IllegalStateException("The realm's user profile has no email attribute");
  }
}
