package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.RegisterUserUseCase;
import eu.talmar.api.application.port.in.command.RegisterUserCommand;
import eu.talmar.api.application.port.out.CompanyRepository;
import eu.talmar.api.application.port.out.RegistrationMailer;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.Company;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.Registrant;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a user with a work e-mail address (ADR 0003). The e-mail domain decides the company: an
 * unknown domain founds a new company with the user as owner, a known one makes the user a member
 * of it.
 *
 * <p>Whether the address is already registered never shows in the outcome, only in which mail is
 * sent: an unconfirmed registration gets a fresh verification link, a confirmed account a mail that
 * the account exists. The details of a repeated registration (name, job role) are ignored; the
 * first registration stands.
 *
 * <p>The request is already validated against the contract by the REST adapter, including the rule
 * that public mail domains are not accepted; this service only applies the business rules.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {
  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final VerificationTokenIssuer verificationTokenIssuer;
  private final RegistrationMailer registrationMailer;
  private final Clock clock;

  @Override
  @Transactional
  public void register(RegisterUserCommand command) {
    var now = LocalDateTime.now(clock);

    userRepository
        .findByEmail(command.email())
        .ifPresentOrElse(
            existingUser -> registerAgain(existingUser, now), () -> registerNewUser(command, now));
  }

  private void registerNewUser(RegisterUserCommand command, LocalDateTime now) {
    var emailDomain = command.email().domain();
    var userId = User.newId();
    Optional<Company> existingCompany = companyRepository.findByEmailDomain(emailDomain);
    Company company = existingCompany.orElseGet(() -> foundCompany(emailDomain, userId, now));

    var registrant =
        new Registrant(command.firstName(), command.lastName(), command.email(), command.jobRole());
    var issuedToken = verificationTokenIssuer.issue();
    var user =
        User.register(
            userId,
            registrant,
            company.getId(),
            existingCompany.isPresent(),
            issuedToken.token(),
            now);
    userRepository.save(user);

    log.info("User is registered: {}", user);
    registrationMailer.sendVerificationMail(user, issuedToken.rawToken());
  }

  private Company foundCompany(String emailDomain, UUID founderId, LocalDateTime now) {
    var company = Company.found(emailDomain, founderId, now);
    companyRepository.save(company);
    log.info("Company is founded: {}", company);
    return company;
  }

  private void registerAgain(User existingUser, LocalDateTime now) {
    if (!existingUser.isAwaitingVerification()) {
      log.info("Registration for an existing account: {}", existingUser);
      registrationMailer.sendAccountExistsMail(existingUser);
      return;
    }
    var issuedToken = verificationTokenIssuer.issue();
    existingUser.renewVerificationToken(issuedToken.token(), now);
    userRepository.save(existingUser);

    log.info("Verification token is renewed: {}", existingUser);
    registrationMailer.sendVerificationMail(existingUser, issuedToken.rawToken());
  }
}
