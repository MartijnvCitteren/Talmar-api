package eu.talmar.api.adapter.out.blocklist;

import eu.talmar.api.application.port.out.PublicEmailDomains;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * The block list in {@code identity/public-email-domains.txt} on the classpath, read once at
 * startup, so it can be extended without code. The Keycloak realm enforces the same list on every
 * account; {@code KeycloakRealmTest} keeps the two in sync (ADR 0006, section 3).
 *
 * <p>One domain per line; {@code #} starts a comment. An entry ending in {@code .*} matches the
 * name under every top-level domain ({@code hotmail.*} matches {@code hotmail.com} and {@code
 * hotmail.co.uk}), because the big providers register their name in many countries.
 */
@Component
public final class ClasspathPublicEmailDomains implements PublicEmailDomains {
  static final String RESOURCE = "identity/public-email-domains.txt";
  private static final String ANY_TOP_LEVEL_DOMAIN = "*";

  private final Set<String> domains;
  private final List<String> namesUnderAnyTopLevelDomain;

  public ClasspathPublicEmailDomains() {
    this(new ClassPathResource(RESOURCE));
  }

  ClasspathPublicEmailDomains(Resource resource) {
    var entries = readEntries(resource);
    domains =
        Set.copyOf(
            entries.stream().filter(entry -> !entry.endsWith(ANY_TOP_LEVEL_DOMAIN)).toList());
    namesUnderAnyTopLevelDomain =
        entries.stream()
            .filter(entry -> entry.endsWith(ANY_TOP_LEVEL_DOMAIN))
            .map(entry -> entry.substring(0, entry.length() - ANY_TOP_LEVEL_DOMAIN.length()))
            .toList();
  }

  @Override
  public boolean contains(String emailDomain) {
    return domains.contains(emailDomain)
        || namesUnderAnyTopLevelDomain.stream().anyMatch(emailDomain::startsWith);
  }

  private static List<String> readEntries(Resource resource) {
    try (var reader =
        new BufferedReader(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      return reader
          .lines()
          .map(line -> line.replaceFirst("#.*", "").trim().toLowerCase(Locale.ROOT))
          .filter(line -> !line.isEmpty())
          .toList();
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read the public e-mail domain list " + RESOURCE, e);
    }
  }
}
