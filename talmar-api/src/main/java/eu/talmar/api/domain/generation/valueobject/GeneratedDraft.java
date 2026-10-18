package eu.talmar.api.domain.generation.valueobject;

/**
 * The texts a model drafted for a vacancy description. Only the sections the model has facts for
 * are generated; {@code whatWeOffer} and {@code aboutUs} are deliberately absent until company data
 * exists, so the model is never asked to invent them.
 */
public record GeneratedDraft(String summary, String jobDescription, String tasks) {}
