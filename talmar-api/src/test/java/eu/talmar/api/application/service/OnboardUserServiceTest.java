package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.IdentityFactory.EMAIL;
import static eu.talmar.api.testSupport.IdentityFactory.EMAIL_DOMAIN;
import static eu.talmar.api.testSupport.IdentityFactory.FIRST_NAME;
import static eu.talmar.api.testSupport.IdentityFactory.JOB_ROLE;
import static eu.talmar.api.testSupport.IdentityFactory.LAST_NAME;
import static eu.talmar.api.testSupport.IdentityFactory.ORGANIZATION_ID;
import static eu.talmar.api.testSupport.IdentityFactory.SUBJECT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.OnboardUserCommand;
import eu.talmar.api.application.port.out.CompanyRepository;
import eu.talmar.api.application.port.out.IdentityProfile;
import eu.talmar.api.application.port.out.IdentityProvider;
import eu.talmar.api.application.port.out.IdentityProviderUnavailableException;
import eu.talmar.api.application.port.out.PublicEmailDomains;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.OnboardingNotAllowedException;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.UserAlreadyOnboardedException;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.shared.Constants;
import eu.talmar.api.testSupport.IdentityFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

@ExtendWith(MockitoExtension.class)
class OnboardUserServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneId.of(Constants.AMS_TIME_ZONE_ID));
  private static final OnboardUserCommand COMMAND = new OnboardUserCommand(SUBJECT, JOB_ROLE);
  private static final IdentityProfile VERIFIED_PROFILE =
      new IdentityProfile(FIRST_NAME, LAST_NAME, new EmailAddress(EMAIL), true);

  @Mock private UserRepository userRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private IdentityProvider identityProvider;
  @Mock private PublicEmailDomains publicEmailDomains;
  @Spy private TransactionOperations transactions = TransactionOperations.withoutTransaction();
  @Spy private Clock clock = CLOCK;

  @InjectMocks private OnboardUserService service;

  @Test
  @DisplayName(
      "given the first account of a domain, when onboarded then the founder and company are saved"
          + " in one transaction with the verified profile, the company is linked, and the founder"
          + " joins its organization")
  void givenFirstAccountOfDomainWhenOnboardedThenFounderCompanyAndOrganizationAreSetUp() {
    givenNewAccount(VERIFIED_PROFILE);
    when(companyRepository.findByEmailDomain(EMAIL_DOMAIN)).thenReturn(Optional.empty());
    var savedCompany = givenCompanyIsReadBackAfterSave();
    when(identityProvider.findOrCreateOrganization(any(), any())).thenReturn(ORGANIZATION_ID);

    var status = service.onboard(COMMAND);

    assertEquals(UserStatus.ACTIVE, status);
    var founder = savedFounder();
    assertEquals(SUBJECT, founder.getSubject());
    assertEquals(VERIFIED_PROFILE.firstName(), founder.getFirstName());
    assertEquals(VERIFIED_PROFILE.email(), founder.getEmail());
    assertEquals(COMMAND.jobRole(), founder.getJobRole());
    assertEquals(Membership.OWNER, founder.getMembership());
    assertEquals(founder.getId(), savedCompany.getValue().getLastModifiedBy());
    assertEquals(LocalDateTime.now(clock), founder.getCreatedAt());
    verify(identityProvider)
        .findOrCreateOrganization(EMAIL_DOMAIN, savedCompany.getValue().getId());
    verify(identityProvider).addOrganizationMember(ORGANIZATION_ID, SUBJECT);
    assertEquals(ORGANIZATION_ID, savedCompany.getValue().getOrganizationId());
    verify(transactions).execute(any());
  }

  @Test
  @DisplayName(
      "given a domain whose company is linked, when onboarded then the account joins as member"
          + " pending approval and nothing is asked of the identity provider but the profile")
  void givenDomainWhoseCompanyIsLinkedWhenOnboardedThenMemberPendingApproval() {
    var company = IdentityFactory.linkedCompany();
    givenNewAccount(VERIFIED_PROFILE);
    when(companyRepository.findByEmailDomain(EMAIL_DOMAIN)).thenReturn(Optional.of(company));
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

    var status = service.onboard(COMMAND);

    assertEquals(UserStatus.PENDING_APPROVAL, status);
    verify(companyRepository, never()).save(any());
    verify(identityProvider, never()).findOrCreateOrganization(any(), any());
    verify(identityProvider, never()).addOrganizationMember(any(), any());
  }

  @Test
  @DisplayName("given an onboarded account, when onboarded again then refused and nothing changes")
  void givenOnboardedAccountWhenOnboardedAgainThenRefusedAndNothingChanges() {
    var company = IdentityFactory.linkedCompany();
    var owner = IdentityFactory.organizationMemberOwner(company.getId());
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(owner));
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));

    assertThrows(UserAlreadyOnboardedException.class, () -> service.onboard(COMMAND));

    verify(identityProvider, never()).getProfile(anyString());
    verify(userRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "given an owner whose company was not linked yet, when onboarded again then it finishes"
          + " without fetching the profile again")
  void givenOwnerWhoseCompanyWasNotLinkedWhenOnboardedAgainThenItFinishes() {
    var company = IdentityFactory.company();
    var owner = IdentityFactory.owner(company.getId());
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(owner));
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    when(identityProvider.findOrCreateOrganization(EMAIL_DOMAIN, company.getId()))
        .thenReturn(ORGANIZATION_ID);

    var status = service.onboard(COMMAND);

    assertEquals(UserStatus.ACTIVE, status);
    verify(identityProvider, never()).getProfile(anyString());
    verify(companyRepository).save(company);
    verify(identityProvider).addOrganizationMember(ORGANIZATION_ID, SUBJECT);
    verify(userRepository).save(owner);
  }

  @Test
  @DisplayName(
      "given a member whose founder's onboarding stopped before the link, when onboarded then the"
          + " member's onboarding links the company but does not add the member")
  void givenMemberWhoseFounderStoppedBeforeLinkWhenOnboardedThenCompanyIsLinkedWithoutMember() {
    var company = IdentityFactory.company();
    givenNewAccount(VERIFIED_PROFILE);
    when(companyRepository.findByEmailDomain(EMAIL_DOMAIN)).thenReturn(Optional.of(company));
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    when(identityProvider.findOrCreateOrganization(EMAIL_DOMAIN, company.getId()))
        .thenReturn(ORGANIZATION_ID);

    var status = service.onboard(COMMAND);

    assertEquals(UserStatus.PENDING_APPROVAL, status);
    verify(companyRepository).save(company);
    verify(identityProvider, never()).addOrganizationMember(any(), any());
  }

  @Test
  @DisplayName("given an unverified e-mail address, when onboarded then refused and nothing saved")
  void givenUnverifiedEmailAddressWhenOnboardedThenRefusedAndNothingSaved() {
    givenNewAccount(new IdentityProfile(FIRST_NAME, LAST_NAME, new EmailAddress(EMAIL), false));

    assertThrows(OnboardingNotAllowedException.class, () -> service.onboard(COMMAND));

    verify(userRepository, never()).save(any());
    verify(companyRepository, never()).save(any());
  }

  @Test
  @DisplayName("given a public mail domain, when onboarded then refused and nothing saved")
  void givenPublicMailDomainWhenOnboardedThenRefusedAndNothingSaved() {
    givenNewAccount(VERIFIED_PROFILE);
    when(publicEmailDomains.contains(EMAIL_DOMAIN)).thenReturn(true);

    assertThrows(OnboardingNotAllowedException.class, () -> service.onboard(COMMAND));

    verify(userRepository, never()).save(any());
    verify(companyRepository, never()).save(any());
  }

  @Test
  @DisplayName(
      "given the identity provider fails to add the member, when onboarded then the failure"
          + " surfaces and the membership is not recorded")
  void givenIdentityProviderFailsToAddMemberWhenOnboardedThenMembershipIsNotRecorded() {
    var company = IdentityFactory.linkedCompany();
    var owner = IdentityFactory.owner(company.getId());
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.of(owner));
    when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
    var failure = new IdentityProviderUnavailableException("add organization member");
    doThrow(failure).when(identityProvider).addOrganizationMember(ORGANIZATION_ID, SUBJECT);

    assertThrows(IdentityProviderUnavailableException.class, () -> service.onboard(COMMAND));

    verify(userRepository, never()).save(any());
  }

  private void givenNewAccount(IdentityProfile profile) {
    when(userRepository.findBySubject(SUBJECT)).thenReturn(Optional.empty());
    when(identityProvider.getProfile(SUBJECT)).thenReturn(profile);
  }

  /** Captures the founded company and serves it from {@code findById}, as the database would. */
  private ArgumentCaptor<Company> givenCompanyIsReadBackAfterSave() {
    var savedCompany = ArgumentCaptor.forClass(Company.class);
    doNothing().when(companyRepository).save(savedCompany.capture());
    when(companyRepository.findById(any(UUID.class)))
        .thenAnswer(invocation -> Optional.of(savedCompany.getValue()));
    return savedCompany;
  }

  private User savedFounder() {
    var savedUser = ArgumentCaptor.forClass(User.class);
    verify(userRepository, atLeastOnce()).save(savedUser.capture());
    return savedUser.getValue();
  }
}
