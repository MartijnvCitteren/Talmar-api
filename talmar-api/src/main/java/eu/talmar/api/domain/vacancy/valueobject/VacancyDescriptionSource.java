package eu.talmar.api.domain.vacancy.valueobject;

/**
 * Source of the vacancy description.
 *
 * <p>{@code MANUAL}: set by user via {@code POST /vacancy/{id}/description}.
 *
 * <p>{@code GENERATED}: reserved for future AI-assisted generation path; currently not reachable
 * under the contract (see ADR 0004 Decision 2).
 */
public enum VacancyDescriptionSource {
  MANUAL,
  GENERATED
}
