package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.OnboardUserUseCase;
import eu.talmar.api.application.port.in.command.OnboardUserCommand;
import eu.talmar.api.application.port.out.CompanyRepository;
import eu.talmar.api.application.port.out.IdentityProfile;
import eu.talmar.api.application.port.out.IdentityProvider;
import eu.talmar.api.application.port.out.PublicEmailDomains;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.OnboardingNotAllowedException;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.UserAlreadyOnboardedException;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Onboards a logged-in account (ADR 0006, sections 2 and 7): the e-mail domain of its verified
 * profile decides the company, the first user of a domain creates it and becomes its owner, later
 * users join as members pending approval.
 *
 * <p>The identity provider and our database cannot share a transaction, so onboarding is built to
 * be repeated until it is complete: the user, the link between company and organization, and an
 * owner's organization membership. Each step that is done is saved before the next one calls the
 * identity provider, and a repeat skips it. No transaction is open during an identity-provider
 * call.
 *
 * <p>Only verified work addresses onboard. The identity provider already refuses public mail
 * domains (ADR 0006, section 3); checking again here means one mistake in its configuration does
 * not let such an address found or join a company.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class OnboardUserService implements OnboardUserUseCase {
  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final IdentityProvider identityProvider;
  private final PublicEmailDomains publicEmailDomains;
  private final TransactionOperations transactions;
  private final Clock clock;

  @Override
  public UserStatus onboard(OnboardUserCommand command) {
    var existingUser = userRepository.findBySubject(command.subject());
    if (existingUser.isPresent() && isComplete(existingUser.get())) {
      throw new UserAlreadyOnboardedException();
    }
    var user = existingUser.orElseGet(() -> createUser(command));
    var company = linkOrganization(getCompany(user), user);
    addToOrganization(user, company);
    log.info("User is onboarded: {}", user);
    return user.getStatus();
  }

  private boolean isComplete(User user) {
    return getCompany(user).isLinkedToOrganization() && !user.needsOrganizationMembership();
  }

  private User createUser(OnboardUserCommand command) {
    var profile = identityProvider.getProfile(command.subject());
    checkMayOnboard(profile);
    return Objects.requireNonNull(
        transactions.execute(status -> saveCompanyAndUser(command, profile)));
  }

  private void checkMayOnboard(IdentityProfile profile) {
    if (!profile.emailVerified()) {
      throw new OnboardingNotAllowedException("the e-mail address is not verified");
    }
    if (publicEmailDomains.contains(profile.email().domain())) {
      throw new OnboardingNotAllowedException("the e-mail address is on a public mail domain");
    }
  }

  /** One transaction, so a founder is never saved without their company or the other way round. */
  private User saveCompanyAndUser(OnboardUserCommand command, IdentityProfile profile) {
    var now = LocalDateTime.now(clock);
    var userId = User.newId();
    var domain = profile.email().domain();
    var existingCompany = companyRepository.findByEmailDomain(domain);
    var company = existingCompany.orElseGet(() -> Company.create(domain, userId, now));
    if (existingCompany.isEmpty()) {
      companyRepository.save(company);
    }
    var user =
        User.create(
            userId,
            command.subject(),
            profile.firstName(),
            profile.lastName(),
            profile.email(),
            command.jobRole(),
            company.getId(),
            existingCompany.isPresent(),
            now);
    userRepository.save(user);
    return user;
  }

  private Company getCompany(User user) {
    return companyRepository.findById(user.getCompanyId()).orElseThrow();
  }

  /**
   * Belongs to the company, not to its founder: if the founder's onboarding stopped before this
   * step and a colleague onboards first, the colleague's request links it.
   */
  private Company linkOrganization(Company company, User user) {
    if (company.isLinkedToOrganization()) {
      return company;
    }
    var organizationId =
        identityProvider.findOrCreateOrganization(company.getEmailDomain(), company.getId());
    company.linkOrganization(organizationId, user.getId(), LocalDateTime.now(clock));
    companyRepository.save(company);
    return company;
  }

  /** Only an owner; a member joins the organization when the owner approves them. */
  private void addToOrganization(User user, Company company) {
    if (!user.needsOrganizationMembership()) {
      return;
    }
    identityProvider.addOrganizationMember(company.getOrganizationId(), user.getSubject());
    user.markOrganizationMember(LocalDateTime.now(clock));
    userRepository.save(user);
  }
}
