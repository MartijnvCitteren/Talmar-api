package eu.talmar.api.domain.identity;

import eu.talmar.api.domain.BaseObject;
import eu.talmar.api.domain.UuidV7Generator;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.Registrant;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.domain.identity.valueobject.VerificationToken;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A person with a Talmar account, always belonging to exactly one {@link Company}. Its id becomes
 * the {@code sub} claim once login exists.
 *
 * <p>Registration is a two-step process the aggregate guards: {@link #register} creates the user as
 * {@code PENDING_VERIFICATION} with a verification token, and {@link #confirm} proves the e-mail
 * address with that token and sets the credentials. Only the company's founder becomes {@code
 * ACTIVE} right away; everyone who joins an existing company waits in {@code PENDING_APPROVAL} for
 * its owner, so nobody gets into a company's data just by owning a mailbox on its domain.
 */
public class User extends BaseObject {

  private final UUID id;
  private final String firstName;
  private final String lastName;
  private final EmailAddress email;
  private final JobRole jobRole;
  private final UUID companyId;
  private final Membership membership;
  private UserStatus status;
  private UserCredentials credentials;
  private VerificationToken verificationToken;

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
    this.credentials = builder.credentials;
    this.verificationToken = builder.verificationToken;
  }

  /**
   * Registers a new user who still has to confirm their e-mail address.
   *
   * @param id the user's id, generated up front with {@link #newId()} so that a company founded by
   *     this registration can record it before the user is saved
   * @param registrant the profile from the registration form
   * @param companyId the company the e-mail domain belongs to
   * @param companyAlreadyExisted whether that company existed before this registration; decides
   *     whether the user becomes its {@code OWNER} or a {@code MEMBER}
   * @param verificationToken the token sent in the verification mail
   * @param registeredAt when the registration happened
   */
  public static User register(
      UUID id,
      Registrant registrant,
      UUID companyId,
      boolean companyAlreadyExisted,
      VerificationToken verificationToken,
      LocalDateTime registeredAt) {
    return builder()
        .id(id)
        .firstName(registrant.firstName())
        .lastName(registrant.lastName())
        .email(registrant.email())
        .jobRole(registrant.jobRole())
        .companyId(companyId)
        .membership(companyAlreadyExisted ? Membership.MEMBER : Membership.OWNER)
        .status(UserStatus.PENDING_VERIFICATION)
        .verificationToken(
            Objects.requireNonNull(verificationToken, "verificationToken is required"))
        .createdAt(registeredAt)
        .build();
  }

  /** A new user id: a UUIDv7, like every id in the domain. */
  public static UUID newId() {
    return UuidV7Generator.getUUID();
  }

  /** Whether the user still has to confirm their e-mail address. */
  public boolean isAwaitingVerification() {
    return status == UserStatus.PENDING_VERIFICATION;
  }

  /**
   * Replaces the verification token, e.g. when someone registers again with an address that is not
   * confirmed yet. The old link stops working.
   *
   * @throws IllegalStateException when the e-mail address is already confirmed
   */
  public void renewVerificationToken(VerificationToken newToken, LocalDateTime renewedAt) {
    if (!isAwaitingVerification()) {
      throw new IllegalStateException("Only an unconfirmed user gets a new verification token");
    }
    this.verificationToken = Objects.requireNonNull(newToken, "newToken is required");
    setLastModifiedAt(renewedAt);
  }

  /**
   * Whether the user can be confirmed with the given token at the given moment: the user is still
   * unconfirmed, the token matches and has not expired.
   */
  public boolean canBeConfirmedWith(String tokenHash, LocalDateTime moment) {
    return isAwaitingVerification()
        && verificationToken != null
        && verificationToken.matches(tokenHash)
        && !verificationToken.isExpiredAt(moment);
  }

  /**
   * Confirms the e-mail address and sets the credentials. The token is cleared, so it works only
   * once. An owner becomes {@code ACTIVE}; a member becomes {@code PENDING_APPROVAL} until the
   * owner approves them.
   *
   * @throws InvalidVerificationTokenException when {@link #canBeConfirmedWith} does not hold
   */
  public void confirm(String tokenHash, UserCredentials newCredentials, LocalDateTime confirmedAt) {
    if (!canBeConfirmedWith(tokenHash, confirmedAt)) {
      throw new InvalidVerificationTokenException();
    }
    this.credentials = Objects.requireNonNull(newCredentials, "newCredentials is required");
    this.verificationToken = null;
    this.status = membership == Membership.OWNER ? UserStatus.ACTIVE : UserStatus.PENDING_APPROVAL;
    setLastModifiedAt(confirmedAt);
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

  /** The credentials, or {@code null} while the e-mail address is not confirmed. */
  public UserCredentials getCredentials() {
    return credentials;
  }

  /** The verification token, or {@code null} once the e-mail address is confirmed. */
  public VerificationToken getVerificationToken() {
    return verificationToken;
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
    private UserCredentials credentials;
    private VerificationToken verificationToken;
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

    public Builder credentials(UserCredentials credentials) {
      this.credentials = credentials;
      return this;
    }

    public Builder verificationToken(VerificationToken verificationToken) {
      this.verificationToken = verificationToken;
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
