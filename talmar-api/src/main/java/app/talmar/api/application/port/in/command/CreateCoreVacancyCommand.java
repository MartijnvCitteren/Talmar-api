package app.talmar.api.application.port.in.command;

import app.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import app.talmar.api.domain.vacancy.valueobject.Language;
import app.talmar.api.domain.vacancy.valueobject.Location;
import app.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import app.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import lombok.Builder;

@Builder
public record CreateCoreVacancyCommand(
    String jobTitle,
    Language language,
    VacancyCategory category,
    Location location,
    WorkplaceType workplaceType,
    HoursPerWeek hoursPerWeek) {}
