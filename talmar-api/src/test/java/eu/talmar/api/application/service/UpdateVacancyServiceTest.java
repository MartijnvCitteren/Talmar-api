package eu.talmar.api.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.UpdateVacancyCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.domain.vacancy.valueobject.Location;
import eu.talmar.api.domain.vacancy.valueobject.Offer;
import eu.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import eu.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import eu.talmar.api.testSupport.VacancyFactory;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateVacancyServiceTest {

  @Mock private VacancyRepository vacancyRepository;

  @InjectMocks private UpdateVacancyService service;

  @Test
  @DisplayName(
      "given a vacancy, when every part is updated then each part is replaced as a whole and saved")
  void givenVacancyWhenEveryPartUpdatedThenEachPartReplacedAndSaved() {
    var vacancy =
        VacancyFactory.getFilledCoreVacancy()
            .contactPerson(VacancyFactory.CONTACT_PERSON)
            .offer(VacancyFactory.OFFER)
            .build();
    when(vacancyRepository.findById(vacancy.getId())).thenReturn(Optional.of(vacancy));
    var command =
        UpdateVacancyCommand.builder()
            .vacancyId(vacancy.getId())
            .jobTitle("Senior Sales Manager")
            .language(Language.DE)
            .category(VacancyCategory.MANAGEMENT)
            .location(new Location("DE", "Berlin"))
            .workplaceType(WorkplaceType.REMOTE)
            .hoursPerWeek(new HoursPerWeek(new BigDecimal("36"), new BigDecimal("38")))
            .description(
                new VacancyDescription(
                    "Summary", null, null, null, null, VacancyDescriptionSource.MANUAL))
            .contactPerson(
                new ContactPerson(
                    "Sanne de Vries", null, null, VacancyFactory.CONTACT_PERSON.email()))
            .offer(new Offer(null, null, null, null, new BigDecimal("30")))
            .build();

    var result = service.updateVacancy(command);

    assertEquals(command.jobTitle(), result.getJobTitle());
    assertEquals(command.language(), result.getLanguage());
    assertEquals(command.category(), result.getCategory());
    assertEquals(command.location(), result.getLocation());
    assertEquals(command.workplaceType(), result.getWorkplaceType());
    assertEquals(command.hoursPerWeek(), result.getHoursPerWeek());
    assertEquals(command.description(), result.getDescription());
    assertEquals(command.contactPerson(), result.getContactPerson());
    assertEquals(command.offer(), result.getOffer());
    verify(vacancyRepository).save(vacancy);
  }

  @Test
  @DisplayName(
      "given a vacancy, when only the job title is updated then every other part stays as it was")
  void givenVacancyWhenOnlyJobTitleUpdatedThenOtherPartsStay() {
    var vacancy =
        VacancyFactory.getFilledCoreVacancy()
            .contactPerson(VacancyFactory.CONTACT_PERSON)
            .offer(VacancyFactory.OFFER)
            .build();
    when(vacancyRepository.findById(vacancy.getId())).thenReturn(Optional.of(vacancy));
    var command =
        UpdateVacancyCommand.builder()
            .vacancyId(vacancy.getId())
            .jobTitle("Senior Sales Manager")
            .build();

    var result = service.updateVacancy(command);

    assertEquals(command.jobTitle(), result.getJobTitle());
    assertEquals(VacancyFactory.LANGUAGE, result.getLanguage());
    assertEquals(VacancyFactory.CATEGORY, result.getCategory());
    assertEquals(VacancyFactory.WORKPLACE_TYPE, result.getWorkplaceType());
    assertEquals(VacancyFactory.MIN_HOURS_PER_WEEK, result.getHoursPerWeek().minHours());
    assertEquals(VacancyFactory.CONTACT_PERSON, result.getContactPerson());
    assertEquals(VacancyFactory.OFFER, result.getOffer());
    verify(vacancyRepository).save(vacancy);
  }

  @Test
  @DisplayName("given an unknown vacancy id, when updateVacancy then throws and never saves")
  void givenUnknownVacancyIdWhenUpdateVacancyThenThrowsAndNeverSaves() {
    var vacancyId = UUID.randomUUID();
    when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.empty());
    var command = UpdateVacancyCommand.builder().vacancyId(vacancyId).build();

    assertThrows(VacancyNotFoundException.class, () -> service.updateVacancy(command));

    verify(vacancyRepository, never()).save(any());
  }
}
