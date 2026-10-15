package app.jobzy.api.application.port.in.command;

import java.util.UUID;
import lombok.Builder;

/** The user's three answers for drafting the description of an existing vacancy. */
@Builder
public record StartVacancyDescriptionGenerationCommand(
    UUID vacancyId, String mostImportantTasks, String team, String whyNiceJob) {}
