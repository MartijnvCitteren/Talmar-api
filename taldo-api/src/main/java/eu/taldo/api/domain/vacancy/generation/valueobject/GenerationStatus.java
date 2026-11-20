package eu.taldo.api.domain.vacancy.generation.valueobject;

/** Lifecycle of a description generation: {@code PENDING} ends in exactly one final state. */
public enum GenerationStatus {
  PENDING,
  COMPLETED,
  FAILED
}
