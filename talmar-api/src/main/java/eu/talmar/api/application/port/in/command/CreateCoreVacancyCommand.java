package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.domain.vacancy.valueobject.Location;
import eu.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import eu.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import lombok.Builder;

@Builder
public record CreateCoreVacancyCommand(
    String jobTitle,
    Language language,
    VacancyCategory category,
    Location location,
    WorkplaceType workplaceType,
    HoursPerWeek hoursPerWeek) {}
