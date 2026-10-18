package eu.talmar.api.domain.generation.valueobject;

/**
 * Why a generation ended in {@code FAILED}. The distinction matters to the user: {@link
 * #INPUT_REJECTED} means the input has to change, {@link #PROVIDER_UNAVAILABLE} means AI generation
 * is down until we fix it, and for every other reason starting a new generation may succeed.
 */
public enum GenerationFailureReason {
  /** The AI provider kept failing with an error worth retrying, until the retries ran out. */
  PROVIDER_ERROR,
  /**
   * The AI provider refused the request in a way a retry does not fix (invalid key, unknown model,
   * a request it cannot process). Needs a fix on our side.
   */
  PROVIDER_UNAVAILABLE,
  /** The generation did not finish in time, see {@code VacancyDescriptionGeneration}. */
  TIMEOUT,
  /** The model judged the input not to describe a job (off-topic or an injection attempt). */
  INPUT_REJECTED,
  /** The model's output could not be parsed or broke the content rules for a description. */
  INVALID_OUTPUT
}
