package eu.talmar.api.domain.generation.valueobject;

import eu.talmar.api.domain.vacancy.valueobject.Language;

/**
 * Snapshot of everything a generation is based on: the vacancy's job title and language at the
 * moment the generation was started, plus the user's three answers. A snapshot, not a reference, so
 * a later change to the vacancy never changes what a generation was based on.
 */
public record GenerationInput(
    String jobTitle,
    Language language,
    String mostImportantTasks,
    String team,
    String whyNiceJob) {}
