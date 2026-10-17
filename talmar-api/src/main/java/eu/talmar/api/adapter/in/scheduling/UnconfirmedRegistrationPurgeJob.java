package eu.talmar.api.adapter.in.scheduling;

import eu.talmar.api.application.port.in.PurgeUnconfirmedRegistrationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the purge of unconfirmed registrations every {@code talmar.identity.purge-interval} (default
 * one hour). The first run waits one interval too, so it never races a test or a startup. Safe on
 * several instances at once: a user deleted by one instance is simply not found by another.
 */
@Component
@RequiredArgsConstructor
public class UnconfirmedRegistrationPurgeJob {
  private final PurgeUnconfirmedRegistrationsUseCase purgeUnconfirmedRegistrationsUseCase;

  @Scheduled(
      fixedDelayString = "${talmar.identity.purge-interval:PT1H}",
      initialDelayString = "${talmar.identity.purge-interval:PT1H}")
  public void purgeUnconfirmedRegistrations() {
    purgeUnconfirmedRegistrationsUseCase.purgeUnconfirmedRegistrations();
  }
}
