package eu.talmar.api.application.service;

import eu.talmar.api.application.port.in.ChangeNameUseCase;
import eu.talmar.api.application.port.in.command.ChangeNameCommand;
import eu.talmar.api.application.port.out.IdentityProvider;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.UserNotOnboardedException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * Changes a user's names (ADR 0006, section 5). The identity provider is the source for login and
 * keeps the names users see there (mails, the login page), so it is changed first; Talmar's copy in
 * {@code app_user} follows. When the provider fails, nothing has changed. When saving the copy
 * fails after that, the names differ until the user repeats the request, which sends the same names
 * again and brings both back in line.
 *
 * <p>Both names always go to the provider, also when the request changes only one: that also
 * repairs a provider profile that drifted from the copy.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ChangeNameService implements ChangeNameUseCase {
  private final UserRepository userRepository;
  private final IdentityProvider identityProvider;
  private final Clock clock;

  @Override
  public void changeName(ChangeNameCommand command) {
    var user =
        userRepository.findBySubject(command.subject()).orElseThrow(UserNotOnboardedException::new);
    if (command.isEmpty()) {
      return;
    }
    var firstName = Objects.requireNonNullElse(command.firstName(), user.getFirstName());
    var lastName = Objects.requireNonNullElse(command.lastName(), user.getLastName());

    identityProvider.changeName(command.subject(), firstName, lastName);
    user.changeName(firstName, lastName, LocalDateTime.now(clock));
    userRepository.save(user);
    log.info("User changed their name: {}", user);
  }
}
