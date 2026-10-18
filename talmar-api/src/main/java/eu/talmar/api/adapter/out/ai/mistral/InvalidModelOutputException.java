package eu.talmar.api.adapter.out.ai.mistral;

import eu.talmar.api.shared.exception.BaseException;

/**
 * The model's answer cannot be used as a draft. Internal to the adapter: it becomes {@code
 * INVALID_OUTPUT} and never leaves {@link MistralVacancyDescriptionGenerator}.
 */
class InvalidModelOutputException extends BaseException {

  /**
   * @param problem what is wrong with the answer; never contains the answer itself
   */
  InvalidModelOutputException(String problem) {
    super("Model output " + problem);
  }
}
