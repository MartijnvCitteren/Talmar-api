package app.jobzy.api.application.service;

import app.jobzy.api.application.port.in.CompleteVacancyDescriptionGenerationUseCase;
import app.jobzy.api.domain.generation.VacancyDescriptionGenerationStarted;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Hands a started generation to a background thread once the starting transaction has committed.
 *
 * <p>{@code AFTER_COMMIT} guarantees the {@code PENDING} row exists before the work starts, so a
 * poll never sees a {@code 404} for a generation that was just started, and a rolled-back start
 * never calls the AI provider. {@code @Async} runs on the application task executor, which uses
 * virtual threads ({@code spring.threads.virtual.enabled}): the thread mostly waits on the
 * provider, which is exactly what virtual threads are cheap for.
 */
@Component
@RequiredArgsConstructor
public class VacancyDescriptionGenerationStartedListener {
  private final CompleteVacancyDescriptionGenerationUseCase completeUseCase;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onGenerationStarted(VacancyDescriptionGenerationStarted event) {
    completeUseCase.complete(event.generationId());
  }
}
