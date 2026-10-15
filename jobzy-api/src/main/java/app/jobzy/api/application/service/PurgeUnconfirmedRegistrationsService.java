package app.jobzy.api.application.service;

import app.jobzy.api.application.port.in.PurgeUnconfirmedRegistrationsUseCase;
import app.jobzy.api.application.port.out.CompanyRepository;
import app.jobzy.api.application.port.out.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes registrations that were never confirmed (ADR 0003): personal data of someone who never
 * proved the address is theirs is not kept longer than the verification link is valid. A company
 * founded by such a registration goes with it when no other user is left, so the domain becomes
 * free for a real first registrant.
 *
 * <p>A user who registers again gets a new token with a new expiry, so the retention runs from the
 * latest verification mail, not from the first registration.
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class PurgeUnconfirmedRegistrationsService implements PurgeUnconfirmedRegistrationsUseCase {
  private final UserRepository userRepository;
  private final CompanyRepository companyRepository;
  private final Clock clock;

  @Override
  @Transactional
  public int purgeUnconfirmedRegistrations() {
    var expiredUsers = userRepository.findAwaitingVerificationExpiredAt(LocalDateTime.now(clock));

    for (var user : expiredUsers) {
      userRepository.delete(user);
      if (userRepository.countByCompanyId(user.getCompanyId()) == 0) {
        companyRepository.deleteById(user.getCompanyId());
        log.info("Company without users is deleted: {}", user.getCompanyId());
      }
    }

    log.info("Unconfirmed registrations purged: {}", expiredUsers.size());
    return expiredUsers.size();
  }
}
