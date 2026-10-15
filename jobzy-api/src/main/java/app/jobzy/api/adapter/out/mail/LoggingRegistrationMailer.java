package app.jobzy.api.adapter.out.mail;

import app.jobzy.api.application.port.out.RegistrationMailer;
import app.jobzy.api.domain.identity.User;
import app.jobzy.api.shared.config.IdentityProperties;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Stand-in for a real mail provider (ADR 0003): writes the mail to the log instead of sending it,
 * so registration can be tested end to end without recurring cost. An EU mail provider replaces it
 * behind the same port.
 *
 * <p>The log names the user by id, never by e-mail address. The confirmation link does end up in
 * the log, because that is the only way to confirm a registration until real mail exists; treat the
 * log as holding secrets while this adapter is active.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class LoggingRegistrationMailer implements RegistrationMailer {
  private final IdentityProperties identityProperties;

  @Override
  public void sendVerificationMail(User user, String rawToken) {
    log.info("Verification mail for user {}: {}", user.getId(), confirmationLink(rawToken));
  }

  @Override
  public void sendAccountExistsMail(User user) {
    log.info("Account-exists mail for user {}", user.getId());
  }

  URI confirmationLink(String rawToken) {
    return UriComponentsBuilder.fromUri(identityProperties.confirmationUrl())
        .queryParam("token", rawToken)
        .build()
        .toUri();
  }
}
