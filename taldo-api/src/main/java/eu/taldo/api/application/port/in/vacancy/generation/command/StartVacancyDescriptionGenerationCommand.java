package eu.taldo.api.application.port.in.vacancy.generation.command;

import java.util.UUID;
import lombok.Builder;

/** The user's three answers for drafting the description of an existing vacancy. */
@Builder
public record StartVacancyDescriptionGenerationCommand(
    UUID vacancyId, String mostImportantTasks, String team, String whyNiceJob) {}
