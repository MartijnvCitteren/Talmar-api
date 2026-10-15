package app.jobzy.api.adapter.out.persistence.vacancy.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import app.jobzy.api.adapter.out.persistence.vacancy.VacancyDescriptionJpaEntity;
import app.jobzy.api.adapter.out.persistence.vacancy.VacancyJpaEntity;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescription;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import app.jobzy.api.testSupport.VacancyFactory;
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
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    VacancyJpaEntity result = mapper.toJpaEntity(vacancy);

    assertInstanceOf(UUID.class, result.getId());
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
}
