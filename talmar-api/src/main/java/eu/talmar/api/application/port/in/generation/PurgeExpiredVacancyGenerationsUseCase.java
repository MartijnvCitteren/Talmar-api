package eu.talmar.api.application.port.in.generation;

public interface PurgeExpiredVacancyGenerationsUseCase {

  /** Deletes every generation requested longer ago than the retention period. */
  void purgeExpired();
}
