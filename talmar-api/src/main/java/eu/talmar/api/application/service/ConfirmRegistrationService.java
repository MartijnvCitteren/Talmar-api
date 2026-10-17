package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.ConfirmRegistrationUseCase;
import eu.talmar.api.application.port.in.command.ConfirmRegistrationCommand;
import eu.talmar.api.application.port.out.PasswordHasher;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.InvalidVerificationTokenException;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.domain.identity.valueobject.VerificationToken;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirms a registration with the token from the verification mail and sets the password (ADR
 * 0003). The REST adapter has already checked the password policy; the token is checked before the
 * password is hashed, so the deliberately slow hash only runs for a request that will succeed.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ConfirmRegistrationService implements ConfirmRegistrationUseCase {
  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;
  private final Clock clock;

  @Override
  @Transactional
  public UserStatus confirm(ConfirmRegistrationCommand command) {
    var tokenHash = VerificationToken.hashOf(command.token());
    var now = LocalDateTime.now(clock);

    var user =
        userRepository
            .findByVerificationTokenHash(tokenHash)
            .filter(candidate -> candidate.canBeConfirmedWith(tokenHash, now))
            .orElseThrow(InvalidVerificationTokenException::new);

    user.confirm(tokenHash, new UserCredentials(passwordHasher.hash(command.password())), now);
    userRepository.save(user);

    log.info("Registration is confirmed: {}", user);
    return user.getStatus();
  }
}
