package app.jobzy.api.application.port.in.command;

import app.jobzy.api.domain.vacancy.valueobject.HoursPerWeek;
import app.jobzy.api.domain.vacancy.valueobject.Language;
import app.jobzy.api.domain.vacancy.valueobject.Location;
import app.jobzy.api.domain.vacancy.valueobject.VacancyCategory;
import app.jobzy.api.domain.vacancy.valueobject.WorkplaceType;
import lombok.Builder;

@Builder
public record CreateCoreVacancyCommand(
    String jobTitle,
    Language language,
    VacancyCategory category,
    Location location,
    WorkplaceType workplaceType,
    HoursPerWeek hoursPerWeek) {}
