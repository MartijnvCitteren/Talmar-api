package eu.talmar.api.application.port.in.command;

import java.util.UUID;
import lombok.Builder;

@Builder
public record SetManualVacancyDescriptionCommand(
    UUID vacancyId,
    String summary,
    String jobDescription,
    String tasks,
    String whatWeOffer,
    String aboutUs) {}
