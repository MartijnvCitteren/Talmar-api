package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.domain.vacancy.valueobject.Location;
import eu.talmar.api.domain.vacancy.valueobject.Offer;
import eu.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import java.util.UUID;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

/**
 * The parts of an existing vacancy to change. A part that is {@code null} stays as it is; a part
 * that is given replaces the current one as a whole.
 */
@Builder
public record UpdateVacancyCommand(
    UUID vacancyId,
    @Nullable String jobTitle,
    @Nullable Language language,
    @Nullable VacancyCategory category,
    @Nullable Location location,
    @Nullable WorkplaceType workplaceType,
    @Nullable HoursPerWeek hoursPerWeek,
    @Nullable VacancyDescription description,
    @Nullable ContactPerson contactPerson,
    @Nullable Offer offer) {

  /** Redacted to the id: the contact person is personal data and the description is long. */
  @Override
  public String toString() {
    return "UpdateVacancyCommand[vacancyId=" + vacancyId + "]";
  }
}
