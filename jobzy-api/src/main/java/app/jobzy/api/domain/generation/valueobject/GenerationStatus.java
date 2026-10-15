package app.jobzy.api.domain.generation.valueobject;

/** Lifecycle of a description generation: {@code PENDING} ends in exactly one final state. */
public enum GenerationStatus {
  PENDING,
  COMPLETED,
  FAILED
}
