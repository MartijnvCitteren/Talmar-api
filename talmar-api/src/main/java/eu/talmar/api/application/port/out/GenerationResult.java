package eu.talmar.api.application.port.out;

import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;

/** Outcome of one {@link VacancyDescriptionGenerator#generate} call: a draft or a rejection. */
public sealed interface GenerationResult
    permits GenerationResult.Drafted, GenerationResult.Rejected {

  /**
   * The model produced a draft.
   *
   * @param model the model version that produced it, as reported by the provider
   * @param promptVersion the prompt template version used
   */
  record Drafted(GeneratedDraft draft, String model, String promptVersion)
      implements GenerationResult {}

  /** No draft was produced, for the given reason. */
  record Rejected(GenerationFailureReason reason) implements GenerationResult {}
}
