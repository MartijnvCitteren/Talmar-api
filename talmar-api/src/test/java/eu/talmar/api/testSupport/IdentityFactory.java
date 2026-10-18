package eu.talmar.api.testSupport;

import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.Registrant;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.domain.identity.valueobject.VerificationToken;
import java.time.LocalDateTime;
import java.util.UUID;

public class IdentityFactory {
  public static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 9, 12, 0);
  public static final LocalDateTime TOKEN_EXPIRES_AT = NOW.plusHours(48);
  public static final String RAW_TOKEN = "raw-verification-token";
  public static final String TOKEN_HASH = VerificationToken.hashOf(RAW_TOKEN);
  public static final String EMAIL_DOMAIN = "acme.nl";
  public static final String EMAIL = "jane.doe@" + EMAIL_DOMAIN;
  public static final String FIRST_NAME = "Jane";
  public static final String LAST_NAME = "Doe";
  public static final JobRole JOB_ROLE = JobRole.RECRUITER;
  public static final UUID FOUNDER_ID = UUID.fromString("0199c9a0-0000-7000-8000-000000000001");
  public static final String PASSWORD_HASH = "$argon2id$v=19$m=65536,t=3,p=4$c2FsdA$aGFzaA";

  public static VerificationToken validToken() {
    return new VerificationToken(TOKEN_HASH, TOKEN_EXPIRES_AT);
  }

  public static Registrant registrant() {
    return new Registrant(FIRST_NAME, LAST_NAME, new EmailAddress(EMAIL), JOB_ROLE);
  }

  public static Company company() {
    return Company.found(EMAIL_DOMAIN, FOUNDER_ID, NOW);
  }

  public static User pendingOwner(UUID companyId) {
    return User.register(User.newId(), registrant(), companyId, false, validToken(), NOW);
  }

  public static User pendingMember(UUID companyId) {
    return User.register(User.newId(), registrant(), companyId, true, validToken(), NOW);
  }

  public static User activeOwner(UUID companyId) {
    return activeOwner(companyId, EMAIL);
  }

  public static User activeOwner(UUID companyId, String email) {
    return User.builder()
        .firstName(FIRST_NAME)
        .lastName(LAST_NAME)
        .email(new EmailAddress(email))
        .jobRole(JOB_ROLE)
        .companyId(companyId)
        .membership(Membership.OWNER)
        .status(UserStatus.ACTIVE)
        .credentials(new UserCredentials(PASSWORD_HASH))
        .createdAt(NOW)
        .build();
  }
}
