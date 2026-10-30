package eu.talmar.api.adapter.out.persistence.vacancy.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyDescriptionJpaEntity;
import eu.talmar.api.adapter.out.persistence.vacancy.VacancyJpaEntity;
import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import eu.talmar.api.testSupport.VacancyFactory;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyJpaMapperTest {

  @Mock private VacancyDescriptionJpaMapper descriptionMapper;

  @InjectMocks private VacancyJpaMapperImpl mapper;

  @Test
  @DisplayName("given vacancy, when toJpaEntity then maps all fields")
  void givenVacancyWhenToJpaEntityThenMapsAllFields() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().version(3L).build();

    VacancyJpaEntity result = mapper.toJpaEntity(vacancy);

    assertInstanceOf(UUID.class, result.getId());
    assertEquals(vacancy.getVersion(), result.getVersion());
    assertEquals(vacancy.getJobTitle(), result.getJobTitle());
    assertEquals(vacancy.getLanguage(), result.getLanguage());
    assertEquals(vacancy.getCategory(), result.getCategory());
    assertEquals(vacancy.getLocation(), result.getLocation());
    assertEquals(vacancy.getWorkplaceType(), result.getWorkplaceType());
    assertEquals(vacancy.getHoursPerWeek(), result.getHoursPerWeek());
    assertEquals(vacancy.getStatus(), result.getStatus());
  }

  @Test
  @DisplayName("given null vacancy, when toJpaEntity then returns null")
  void givenNullVacancyWhenToJpaEntityThenReturnsNull() {
    assertNull(mapper.toJpaEntity(null));
  }

  @Test
  @DisplayName(
      "given vacancy entity with a description, when toDomain then maps every field including"
          + " the description")
  void givenVacancyEntityWithDescriptionWhenToDomainThenMapsEveryFieldIncludingDescription() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();
    var vacancyEntity = mapper.toJpaEntity(vacancy);
    vacancyEntity.setVersion(3L);
    var descriptionEntity = new VacancyDescriptionJpaEntity();
    vacancyEntity.setDescription(descriptionEntity);
    var domainDescription =
        new VacancyDescription(
            "Summary",
            "Job description",
            "Tasks",
            "What we offer",
            "About us",
            VacancyDescriptionSource.MANUAL);
    when(descriptionMapper.toDomain(descriptionEntity)).thenReturn(domainDescription);

    var result = mapper.toDomain(vacancyEntity);

    assertEquals(vacancyEntity.getId(), result.getId());
    assertEquals(vacancyEntity.getVersion(), result.getVersion());
    assertEquals(vacancyEntity.getJobTitle(), result.getJobTitle());
    assertEquals(vacancyEntity.getLanguage(), result.getLanguage());
    assertEquals(vacancyEntity.getCategory(), result.getCategory());
    assertEquals(vacancyEntity.getLocation(), result.getLocation());
    assertEquals(vacancyEntity.getWorkplaceType(), result.getWorkplaceType());
    assertEquals(vacancyEntity.getHoursPerWeek(), result.getHoursPerWeek());
    assertEquals(vacancyEntity.getStatus(), result.getStatus());
    assertEquals(domainDescription, result.getDescription());
    assertEquals(vacancyEntity.getCreatedAt(), result.getCreatedAt());
  }

  @Test
  @DisplayName("given vacancy entity with no description, when toDomain then description is null")
  void givenVacancyEntityWithNoDescriptionWhenToDomainThenDescriptionIsNull() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();
    var vacancyEntity = mapper.toJpaEntity(vacancy);

    var result = mapper.toDomain(vacancyEntity);

    assertNull(result.getDescription());
  }

  @Test
  @DisplayName(
      "given a vacancy with a contact person and an offer, when toJpaEntity then the contact"
          + " person is flattened into its columns and the offer is embedded")
  void givenVacancyWithContactPersonAndOfferWhenToJpaEntityThenBothAreMapped() {
    var vacancy =
        VacancyFactory.getFilledCoreVacancy()
            .contactPerson(VacancyFactory.CONTACT_PERSON)
            .offer(VacancyFactory.OFFER)
            .build();

    var result = mapper.toJpaEntity(vacancy);

    var contactPerson = vacancy.getContactPerson();
    assertEquals(contactPerson.name(), result.getContactPersonName());
    assertEquals(contactPerson.role(), result.getContactPersonRole());
    assertEquals(contactPerson.phone().value(), result.getContactPersonPhone());
    assertEquals(contactPerson.email().value(), result.getContactPersonEmail());
    var offer = vacancy.getOffer();
    assertEquals(offer.salaryMin(), result.getOffer().getSalaryMin());
    assertEquals(offer.salaryMax(), result.getOffer().getSalaryMax());
    assertEquals(offer.currency(), result.getOffer().getCurrency());
    assertEquals(offer.salaryPeriod(), result.getOffer().getSalaryPeriod());
    assertEquals(offer.numberOfHolidays(), result.getOffer().getNumberOfHolidays());
  }

  @Test
  @DisplayName(
      "given a vacancy entity with contact person columns and an offer, when toDomain then both"
          + " are rebuilt")
  void givenVacancyEntityWithContactPersonAndOfferWhenToDomainThenBothAreRebuilt() {
    var vacancy =
        VacancyFactory.getFilledCoreVacancy()
            .contactPerson(VacancyFactory.CONTACT_PERSON)
            .offer(VacancyFactory.OFFER)
            .build();
    var vacancyEntity = mapper.toJpaEntity(vacancy);

    var result = mapper.toDomain(vacancyEntity);

    assertEquals(vacancy.getContactPerson(), result.getContactPerson());
    assertEquals(vacancy.getOffer(), result.getOffer());
  }

  @Test
  @DisplayName(
      "given a contact person without a phone number, when mapped both ways then the phone"
          + " number stays null")
  void givenContactPersonWithoutPhoneNumberWhenMappedBothWaysThenPhoneNumberStaysNull() {
    var contactPerson = VacancyFactory.CONTACT_PERSON;
    var withoutPhone = new ContactPerson(contactPerson.name(), null, null, contactPerson.email());
    var vacancy = VacancyFactory.getFilledCoreVacancy().contactPerson(withoutPhone).build();

    var result = mapper.toDomain(mapper.toJpaEntity(vacancy));

    assertEquals(withoutPhone, result.getContactPerson());
  }

  @Test
  @DisplayName(
      "given a vacancy entity without contact person columns, when toDomain then the contact"
          + " person is null")
  void givenVacancyEntityWithoutContactPersonColumnsWhenToDomainThenContactPersonIsNull() {
    var vacancyEntity = mapper.toJpaEntity(VacancyFactory.getFilledCoreVacancy().build());

    var result = mapper.toDomain(vacancyEntity);

    assertNull(result.getContactPerson());
  }
}
