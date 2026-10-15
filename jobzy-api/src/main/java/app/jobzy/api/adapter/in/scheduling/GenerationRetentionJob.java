package app.jobzy.api.adapter.in.scheduling;

import app.jobzy.api.application.port.in.PurgeExpiredGenerationsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the retention clean-up of description generations on a fixed delay. A driving adapter
 * like a controller: the schedule is the trigger, the rule lives in the use case.
 *
 * <p>Every instance runs it; that is harmless because the delete is idempotent.
 */
@Component
@RequiredArgsConstructor
public class GenerationRetentionJob {
  private final PurgeExpiredGenerationsUseCase purgeUseCase;

  @Scheduled(
      initialDelayString = "${jobzy.generation.purge-initial-delay:PT1M}",
      fixedDelayString = "${jobzy.generation.purge-interval:PT1H}")
  public void purgeExpiredGenerations() {
    purgeUseCase.purgeExpired();
  }
}
