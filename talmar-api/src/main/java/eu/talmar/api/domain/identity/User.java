package eu.talmar.api.domain.identity;

import eu.talmar.api.domain.BaseObject;
import eu.talmar.api.domain.UuidV7Generator;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A person with a Talmar account, always belonging to exactly one {@link Company}. The account
 * itself (credentials, e-mail verification, login) lives in the identity provider (ADR 0006).
 *
 * <p>Only the company's founder becomes {@code ACTIVE} right away; everyone who joins an existing
 * company waits in {@code PENDING_APPROVAL} for its owner, so nobody gets into a company's data
 * just by owning a mailbox on its domain.
 */
public class User extends BaseObject {

  private final UUID id;
  private final String firstName;
  private final String lastName;
  private final EmailAddress email;
  private final JobRole jobRole;
  private final UUID companyId;
  private final Membership membership;
  private final UserStatus status;

  private User(Builder builder, UUID id) {
    super(builder.createdAt);
    this.id = id;
    this.firstName = builder.firstName;
    this.lastName = builder.lastName;
    this.email = builder.email;
    this.jobRole = builder.jobRole;
    this.companyId = builder.companyId;
    this.membership = builder.membership;
    this.status = builder.status;
  }

  /**
   * Creates a user for a person whose e-mail address the identity provider has verified.
   *
   * @param id the user's id, generated up front with {@link #newId()} so that a company created by
   *     this user can record it before the user is saved
   * @param companyId the company the e-mail domain belongs to
   * @param companyAlreadyExisted whether that company existed before; decides whether the user
   *     becomes its {@code OWNER} ({@code ACTIVE}) or a {@code MEMBER} ({@code PENDING_APPROVAL})
   * @param createdAt when the user was created
   */
  public static User create(
      UUID id,
      String firstName,
      String lastName,
      EmailAddress email,
      JobRole jobRole,
      UUID companyId,
      boolean companyAlreadyExisted,
      LocalDateTime createdAt) {
    return builder()
        .id(id)
        .firstName(firstName)
        .lastName(lastName)
        .email(email)
        .jobRole(jobRole)
        .companyId(companyId)
        .membership(companyAlreadyExisted ? Membership.MEMBER : Membership.OWNER)
        .status(companyAlreadyExisted ? UserStatus.PENDING_APPROVAL : UserStatus.ACTIVE)
        .createdAt(createdAt)
        .build();
  }

  /** A new user id: a UUIDv7, like every id in the domain. */
  public static UUID newId() {
    return UuidV7Generator.getUUID();
  }

  public UUID getId() {
    return id;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public EmailAddress getEmail() {
    return email;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public UUID getCompanyId() {
    return companyId;
  }

  public Membership getMembership() {
    return membership;
  }

  public UserStatus getStatus() {
    return status;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof User user)) {
      return false;
    }
    return Objects.equals(getId(), user.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  /** Ids and process data only: names and e-mail address are personal data. */
  @Override
  public String toString() {
    return "User{id="
        + id
        + ", companyId="
        + companyId
        + ", membership="
        + membership
        + ", status="
        + status
        + '}';
  }

  /** Builds a user; also used to load one from the database, with an explicit id. */
  public static class Builder {
    private UUID explicitId;
    private String firstName;
    private String lastName;
    private EmailAddress email;
    private JobRole jobRole;
    private UUID companyId;
    private Membership membership;
    private UserStatus status;
    private LocalDateTime createdAt;

    private Builder() {}

    public Builder id(UUID id) {
      this.explicitId = id;
      return this;
    }

    public Builder firstName(String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder lastName(String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder email(EmailAddress email) {
      this.email = email;
      return this;
    }

    public Builder jobRole(JobRole jobRole) {
      this.jobRole = jobRole;
      return this;
    }

    public Builder companyId(UUID companyId) {
      this.companyId = companyId;
      return this;
    }

    public Builder membership(Membership membership) {
      this.membership = membership;
      return this;
    }

    public Builder status(UserStatus status) {
      this.status = status;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public User build() {
      Objects.requireNonNull(firstName, "firstName is required");
      Objects.requireNonNull(lastName, "lastName is required");
      Objects.requireNonNull(email, "email is required");
      Objects.requireNonNull(jobRole, "jobRole is required");
      Objects.requireNonNull(companyId, "companyId is required");
      Objects.requireNonNull(membership, "membership is required");
      Objects.requireNonNull(status, "status is required");
      Objects.requireNonNull(createdAt, "createdAt is required");
      return new User(this, explicitId != null ? explicitId : newId());
    }
  }
}
