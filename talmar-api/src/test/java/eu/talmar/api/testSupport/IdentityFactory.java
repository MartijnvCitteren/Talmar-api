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
  public static final UUID CREATOR_ID = UUID.fromString("0199c9a0-0000-7000-8000-000000000001");
  public static final String SUBJECT = "6f1d2c3b-4a5e-4f60-8a7b-9c0d1e2f3a4b";
  public static final String ORGANIZATION_ID = "2c2786d5-36d6-45e4-98f4-905d026f66a9";

  public static Company company() {
    return Company.create(EMAIL_DOMAIN, CREATOR_ID, NOW);
  }

  public static Company linkedCompany() {
    var company = company();
    company.linkOrganization(ORGANIZATION_ID, CREATOR_ID, NOW);
    return company;
  }

  public static User owner(UUID companyId) {
    return user(companyId, EMAIL, false);
  }

  public static User organizationMemberOwner(UUID companyId) {
    var owner = owner(companyId);
    owner.markOrganizationMember(NOW);
    return owner;
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
        email.equals(EMAIL) ? SUBJECT : UUID.randomUUID().toString(),
        FIRST_NAME,
        LAST_NAME,
        new EmailAddress(email),
        JOB_ROLE,
        companyId,
        companyAlreadyExisted,
        NOW);
  }
}
