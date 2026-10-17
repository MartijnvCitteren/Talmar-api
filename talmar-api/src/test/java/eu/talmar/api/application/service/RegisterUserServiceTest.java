package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.IdentityFactory.EMAIL;
import static eu.talmar.api.testSupport.IdentityFactory.EMAIL_DOMAIN;
import static eu.talmar.api.testSupport.IdentityFactory.FIRST_NAME;
import static eu.talmar.api.testSupport.IdentityFactory.JOB_ROLE;
import static eu.talmar.api.testSupport.IdentityFactory.LAST_NAME;
import static eu.talmar.api.testSupport.IdentityFactory.NOW;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_EXPIRES_AT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.RegisterUserCommand;
import eu.talmar.api.application.port.out.CompanyRepository;
import eu.talmar.api.application.port.out.RegistrationMailer;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.application.service.VerificationTokenIssuer.IssuedVerificationToken;
import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.domain.identity.valueobject.VerificationToken;
import eu.talmar.api.testSupport.IdentityFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-10-09T12:00:00Z"), ZoneOffset.UTC);
  private static final String NEW_RAW_TOKEN = "new-raw-token";
  private static final IssuedVerificationToken ISSUED_TOKEN =
      new IssuedVerificationToken(
          NEW_RAW_TOKEN,
          new VerificationToken(VerificationToken.hashOf(NEW_RAW_TOKEN), TOKEN_EXPIRES_AT));

  @Mock private UserRepository userRepository;
  @Mock private CompanyRepository companyRepository;
  @Mock private VerificationTokenIssuer verificationTokenIssuer;
  @Mock private RegistrationMailer registrationMailer;

  private RegisterUserService service;

  @BeforeEach
  void setUp() {
    service =
        new RegisterUserService(
            userRepository, companyRepository, verificationTokenIssuer, registrationMailer, CLOCK);
  }

  @Test
  @DisplayName(
      "given an unknown e-mail domain, when register then a company named after the domain is"
          + " founded by the user, who becomes its owner and gets a verification mail")
  void givenUnknownEmailDomainWhenRegisterThenCompanyIsFoundedAndUserBecomesOwner() {
    when(companyRepository.findByEmailDomain(EMAIL_DOMAIN)).thenReturn(Optional.empty());
    when(verificationTokenIssuer.issue()).thenReturn(ISSUED_TOKEN);
    var command = command(EMAIL);

    service.register(command);

    var company = ArgumentCaptor.forClass(Company.class);
    verify(companyRepository).save(company.capture());
    var user = savedUser();
    assertEquals("Acme", company.getValue().getName());
    assertEquals(EMAIL_DOMAIN, company.getValue().getEmailDomain());
    assertEquals(user.getId(), company.getValue().getLastModifiedBy());
    assertEquals(NOW, company.getValue().getCreatedAt());

    assertEquals(company.getValue().getId(), user.getCompanyId());
    assertEquals(Membership.OWNER, user.getMembership());
    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    assertEquals(command.email(), user.getEmail());
    assertEquals(command.firstName(), user.getFirstName());
    assertEquals(command.lastName(), user.getLastName());
    assertEquals(command.jobRole(), user.getJobRole());
    assertEquals(ISSUED_TOKEN.token(), user.getVerificationToken());
    verify(registrationMailer).sendVerificationMail(user, NEW_RAW_TOKEN);
  }

  @Test
  @DisplayName(
      "given a known e-mail domain, when register then the user joins that company as a member"
          + " and the company is not changed")
  void givenKnownEmailDomainWhenRegisterThenUserJoinsCompanyAsMember() {
    var existingCompany = IdentityFactory.company();
    when(companyRepository.findByEmailDomain(EMAIL_DOMAIN))
        .thenReturn(Optional.of(existingCompany));
    when(verificationTokenIssuer.issue()).thenReturn(ISSUED_TOKEN);

    service.register(command(EMAIL));

    verify(companyRepository, never()).save(any());
    var user = savedUser();
    assertEquals(existingCompany.getId(), user.getCompanyId());
    assertEquals(Membership.MEMBER, user.getMembership());
    verify(registrationMailer).sendVerificationMail(user, NEW_RAW_TOKEN);
  }

  @Test
  @DisplayName(
      "given an address with an unconfirmed registration, when register again then the token is"
          + " renewed and a new verification mail is sent")
  void givenUnconfirmedRegistrationWhenRegisterAgainThenTokenIsRenewedAndMailSent() {
    var existingUser = IdentityFactory.pendingOwner(UUID.randomUUID());
    when(userRepository.findByEmail(new EmailAddress(EMAIL))).thenReturn(Optional.of(existingUser));
    when(verificationTokenIssuer.issue()).thenReturn(ISSUED_TOKEN);

    service.register(command(EMAIL));

    assertEquals(ISSUED_TOKEN.token(), existingUser.getVerificationToken());
    verify(userRepository).save(existingUser);
    verify(registrationMailer).sendVerificationMail(existingUser, NEW_RAW_TOKEN);
    verifyNoInteractions(companyRepository);
  }

  @Test
  @DisplayName(
      "given an address with a confirmed account, when register again then an account-exists"
          + " mail is sent and nothing is saved")
  void givenConfirmedAccountWhenRegisterAgainThenAccountExistsMailIsSentAndNothingSaved() {
    var existingUser = IdentityFactory.activeOwner(UUID.randomUUID());
    when(userRepository.findByEmail(new EmailAddress(EMAIL))).thenReturn(Optional.of(existingUser));

    service.register(command(EMAIL));

    verify(registrationMailer).sendAccountExistsMail(existingUser);
    verify(registrationMailer, never()).sendVerificationMail(any(), anyString());
    verify(userRepository, never()).save(any());
    verifyNoInteractions(companyRepository, verificationTokenIssuer);
  }

  private User savedUser() {
    var user = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(user.capture());
    return user.getValue();
  }

  private static RegisterUserCommand command(String email) {
    return RegisterUserCommand.builder()
        .firstName(FIRST_NAME)
        .lastName(LAST_NAME)
        .email(new EmailAddress(email))
        .jobRole(JOB_ROLE)
        .build();
  }
}
