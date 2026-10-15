package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.generation.valueobject.GenerationInput;

/**
 * Drafts a vacancy description with an AI model. Implementations wrap one provider and are the only
 * place that knows it.
 *
 * <p>Implementations never throw for a failed generation: every failure (provider error, timeout,
 * rejected input, unusable output) comes back as a {@link GenerationResult.Rejected}, so the caller
 * can record it on the generation.
 */
public interface VacancyDescriptionGenerator {

  GenerationResult generate(GenerationInput input);
}
