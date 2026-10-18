package eu.talmar.api.domain.identity;

import eu.talmar.api.domain.BaseObject;
import eu.talmar.api.domain.UuidV7Generator;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A company that uses Talmar. Identified by its e-mail domain: everyone who registers with an
 * address on that domain belongs to it, which is why the domain is unique and stored in lower case.
 * The first user of a domain creates the company.
 *
 * <p>Each company gets an organization at the identity provider, which scopes access to its data
 * and will carry its SSO settings (ADR 0006, section 4). It is created after the company is saved,
 * because the identity provider cannot join our transaction, so a company can briefly exist without
 * one; {@link #isLinkedToOrganization()} tells an interrupted onboarding to finish the link.
 *
 * <p>A company always records which user last created or changed it ({@link #getLastModifiedBy()}),
 * so every change can be traced back to a person by their user id.
 */
public class Company extends BaseObject {

  /**
   * Second-level labels under which countries register company domains, as in {@code acme.co.uk} or
   * {@code acme.com.au}. Skipped when deriving a name, so these become "Acme" and not "Co".
   */
  private static final Set<String> SECOND_LEVEL_LABELS =
      Set.of("co", "com", "org", "net", "ac", "gov", "edu");

  private final UUID id;
  private final String name;
  private final String emailDomain;
  private String organizationId;

  private Company(Builder builder, UUID id) {
    super(builder.createdAt);
    this.id = id;
    this.name = builder.name;
    this.emailDomain = builder.emailDomain;
    this.organizationId = builder.organizationId;
    setLastModifiedBy(builder.lastModifiedBy);
  }

  /**
   * Creates a new company for an e-mail domain that has no company yet. Nobody is asked for a
   * company name, to keep sign-up short; the company starts with a name derived from the domain,
   * which its users can correct later.
   *
   * @param emailDomain the lower-case e-mail domain of the first user
   * @param createdBy the id of the first user, recorded as the user who last modified it
   * @param createdAt when the first user was created
   */
  public static Company create(String emailDomain, UUID createdBy, LocalDateTime createdAt) {
    return builder()
        .name(nameFrom(emailDomain))
        .emailDomain(emailDomain)
        .lastModifiedBy(createdBy)
        .createdAt(createdAt)
        .build();
  }

  /**
   * Derives a provisional name from the registrable part of an e-mail domain, capitalised: {@code
   * acme.nl} and {@code sales.acme.co.uk} both become "Acme". A heuristic, not a public-suffix
   * lookup: good enough for a first guess the users can correct.
   */
  private static String nameFrom(String emailDomain) {
    List<String> labels = Arrays.asList(emailDomain.split("\\."));
    List<String> withoutTopLevel = labels.subList(0, labels.size() - 1);
    int registrable = withoutTopLevel.size() - 1;
    if (registrable > 0 && SECOND_LEVEL_LABELS.contains(withoutTopLevel.get(registrable))) {
      registrable--;
    }
    String label = withoutTopLevel.get(registrable);
    return label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1);
  }

  public boolean isLinkedToOrganization() {
    return organizationId != null;
  }

  /**
   * Links the company to its organization at the identity provider.
   *
   * @param linkedBy the id of the user whose onboarding created or found the organization
   */
  public void linkOrganization(String organizationId, UUID linkedBy, LocalDateTime linkedAt) {
    this.organizationId = organizationId;
    setLastModifiedBy(linkedBy);
    setLastModifiedAt(linkedAt);
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmailDomain() {
    return emailDomain;
  }

  /** The organization's id at the identity provider, or {@code null} until it is linked. */
  public String getOrganizationId() {
    return organizationId;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Company company)) {
      return false;
    }
    return Objects.equals(getId(), company.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public String toString() {
    return "Company{id=" + id + ", emailDomain='" + emailDomain + "'}";
  }

  /** Builds a company; also used to load one from the database, with an explicit id. */
  public static class Builder {
    private UUID explicitId;
    private String name;
    private String emailDomain;
    private String organizationId;
    private UUID lastModifiedBy;
    private LocalDateTime createdAt;

    private Builder() {}

    public Builder id(UUID id) {
      this.explicitId = id;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder emailDomain(String emailDomain) {
      this.emailDomain = emailDomain;
      return this;
    }

    public Builder organizationId(String organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder lastModifiedBy(UUID lastModifiedBy) {
      this.lastModifiedBy = lastModifiedBy;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Company build() {
      Objects.requireNonNull(name, "name is required");
      Objects.requireNonNull(emailDomain, "emailDomain is required");
      Objects.requireNonNull(lastModifiedBy, "lastModifiedBy is required");
      Objects.requireNonNull(createdAt, "createdAt is required");
      return new Company(this, explicitId != null ? explicitId : UuidV7Generator.getUUID());
    }
  }
}
