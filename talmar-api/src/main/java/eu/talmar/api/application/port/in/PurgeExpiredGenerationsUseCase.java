package eu.talmar.api.application.port.in;

public interface PurgeExpiredGenerationsUseCase {

  /** Deletes every generation requested longer ago than the retention period. */
  void purgeExpired();
}
