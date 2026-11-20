package eu.taldo.api.application.port.in.vacancy.generation;

public interface PurgeExpiredVacancyGenerationsUseCase {

  /** Deletes every generation requested longer ago than the retention period. */
  void purgeExpired();
}
