package eu.talmar.api.adapter.in.rest.identity.validation;

import eu.talmar.api.adapter.in.rest.identity.PublicEmailDomainException;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.identity.adapter.in.web.contract.RegistrationRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Rejects a registration on a public mail domain (gmail, outlook, proton, ...) at the REST adapter
 * edge, before the use case runs. "Work e-mail addresses only" is part of the registration contract
 * (see the {@code email} description in {@code IdentityApi.yml}), the same as the shape of the
 * address; it just needs a list that the YAML cannot hold.
 *
 * <p>The block list is read once at startup from {@code identity/public-email-domains.txt} on the
 * classpath, so it can be extended without code. One domain per line; {@code #} starts a comment.
 * An entry ending in {@code .*} matches the name under every top-level domain ({@code hotmail.*}
 * matches {@code hotmail.com} and {@code hotmail.co.uk}), because the big providers register their
 * name in many countries.
 */
@Component
public final class WorkEmailAddressValidator {
  static final String RESOURCE = "identity/public-email-domains.txt";
  private static final String ANY_TOP_LEVEL_DOMAIN = "*";

  private final Set<String> domains = new HashSet<>();
  private final List<String> namesUnderAnyTopLevelDomain;

  public WorkEmailAddressValidator() {
    this(new ClassPathResource(RESOURCE));
  }

  WorkEmailAddressValidator(Resource resource) {
    var entries = readEntries(resource);
    entries.stream().filter(entry -> !entry.endsWith(ANY_TOP_LEVEL_DOMAIN)).forEach(domains::add);
    namesUnderAnyTopLevelDomain =
        entries.stream()
            .filter(entry -> entry.endsWith(ANY_TOP_LEVEL_DOMAIN))
            .map(entry -> entry.substring(0, entry.length() - ANY_TOP_LEVEL_DOMAIN.length()))
            .toList();
  }

  /**
   * Rejects the request when its e-mail address is on a public mail domain.
   *
   * @param request a request whose {@code email} bean validation has already accepted
   * @throws PublicEmailDomainException if the address is on a public mail domain
   */
  public void validate(RegistrationRequest request) {
    if (isPublic(new EmailAddress(request.getEmail()).domain())) {
      throw new PublicEmailDomainException();
    }
  }

  private boolean isPublic(String domain) {
    return domains.contains(domain)
        || namesUnderAnyTopLevelDomain.stream().anyMatch(domain::startsWith);
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
