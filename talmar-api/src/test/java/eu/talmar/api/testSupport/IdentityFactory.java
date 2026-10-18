package eu.talmar.api.testSupport;

import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import java.time.LocalDateTime;
import java.util.UUID;

public class IdentityFactory {
  public static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 9, 12, 0);
  public static final String EMAIL_DOMAIN = "acme.nl";
  public static final String EMAIL = "jane.doe@" + EMAIL_DOMAIN;
  public static final String FIRST_NAME = "Jane";
  public static final String LAST_NAME = "Doe";
  public static final JobRole JOB_ROLE = JobRole.RECRUITER;
  public static final UUID FOUNDER_ID = UUID.fromString("0199c9a0-0000-7000-8000-000000000001");

  public static Company company() {
    return Company.found(EMAIL_DOMAIN, FOUNDER_ID, NOW);
  }

  public static User owner(UUID companyId) {
    return user(companyId, EMAIL, false);
  }

  public static User member(UUID companyId) {
    return member(companyId, EMAIL);
  }

  public static User member(UUID companyId, String email) {
    return user(companyId, email, true);
  }

  private static User user(UUID companyId, String email, boolean companyAlreadyExisted) {
    return User.create(
        User.newId(),
        FIRST_NAME,
        LAST_NAME,
        new EmailAddress(email),
        JOB_ROLE,
        companyId,
        companyAlreadyExisted,
        NOW);
  }
}
