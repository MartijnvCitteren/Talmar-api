package app.jobzy.api.domain.vacancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.jobzy.api.domain.vacancy.valueobject.HoursPerWeek;
import app.jobzy.api.domain.vacancy.valueobject.Language;
import app.jobzy.api.domain.vacancy.valueobject.Location;
import app.jobzy.api.domain.vacancy.valueobject.VacancyCategory;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescription;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import app.jobzy.api.domain.vacancy.valueobject.VacancyStatus;
import app.jobzy.api.domain.vacancy.valueobject.WorkplaceType;
import app.jobzy.api.testSupport.VacancyFactory;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VacancyTest {

  @Test
  @DisplayName("given builder with an explicit id, when build then vacancy has exactly that id")
  void givenBuilderWithExplicitIdWhenBuildThenVacancyHasExactlyThatId() {
    var id = UUID.randomUUID();

    var vacancy = VacancyFactory.getFilledCoreVacancy().id(id).build();

    assertEquals(id, vacancy.getId());
  }

  @Test
  @DisplayName("given builder with no explicit id, when build then vacancy has a generated id")
  void givenBuilderWithNoExplicitIdWhenBuildThenVacancyHasGeneratedId() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    assertNotNull(vacancy.getId());
  }

  @Test
  @DisplayName("given builder without required fields, when build then throws naming the field")
  void givenBuilderWithoutRequiredFieldsWhenBuildThenThrowsNamingTheField() {
    var builder = Vacancy.builder();

    var exception = assertThrows(NullPointerException.class, builder::build);

    assertEquals("jobTitle is required", exception.getMessage());
  }

  @Test
  @DisplayName("given builder with no status, when build then status defaults to draft")
  void givenBuilderWithNoStatusWhenBuildThenStatusDefaultsToDraft() {
    var vacancy =
        Vacancy.builder()
            .jobTitle("Backend Engineer")
            .language(Language.EN)
            .category(VacancyCategory.ENGINEERING)
            .location(new Location("The Netherlands", "Leiden"))
            .workplaceType(WorkplaceType.REMOTE)
            .hoursPerWeek(new HoursPerWeek(BigDecimal.valueOf(32), BigDecimal.valueOf(40)))
            .createdAt(VacancyFactory.CREATED_AT)
            .build();

    assertEquals(VacancyStatus.DRAFT, vacancy.getStatus());
  }

  @Test
  @DisplayName("given builder with all fields, when build then vacancy exposes those fields")
  void givenBuilderWithAllFieldsWhenBuildThenVacancyExposesThoseFields() {
    var location = new Location("The Netherlands", "Amsterdam");
    var hoursPerWeek = new HoursPerWeek(BigDecimal.valueOf(24), BigDecimal.valueOf(36));

    var vacancy =
        Vacancy.builder()
            .jobTitle("Sales Manager")
            .language(Language.NL)
            .category(VacancyCategory.SALES)
            .location(location)
            .workplaceType(WorkplaceType.HYBRID)
            .hoursPerWeek(hoursPerWeek)
            .status(VacancyStatus.PUBLISHED)
            .createdAt(VacancyFactory.CREATED_AT)
            .build();

    assertEquals("Sales Manager", vacancy.getJobTitle());
    assertEquals(Language.NL, vacancy.getLanguage());
    assertEquals(VacancyCategory.SALES, vacancy.getCategory());
    assertEquals(location, vacancy.getLocation());
    assertEquals(WorkplaceType.HYBRID, vacancy.getWorkplaceType());
    assertEquals(hoursPerWeek, vacancy.getHoursPerWeek());
    assertEquals(VacancyStatus.PUBLISHED, vacancy.getStatus());
    assertEquals(VacancyFactory.CREATED_AT, vacancy.getCreatedAt());
  }

  @Test
  @DisplayName("given builder without createdAt, when build then throws naming the field")
  void givenBuilderWithoutCreatedAtWhenBuildThenThrowsNamingTheField() {
    var builder =
        Vacancy.builder()
            .jobTitle("Backend Engineer")
            .language(Language.EN)
            .category(VacancyCategory.ENGINEERING)
            .location(new Location("The Netherlands", "Leiden"))
            .workplaceType(WorkplaceType.REMOTE)
            .hoursPerWeek(new HoursPerWeek(BigDecimal.valueOf(32), BigDecimal.valueOf(40)));

    var exception = assertThrows(NullPointerException.class, builder::build);

    assertEquals("createdAt is required", exception.getMessage());
  }

  @Test
  @DisplayName("given builder with no description, when build then description defaults to null")
  void givenBuilderWithNoDescriptionWhenBuildThenDescriptionDefaultsToNull() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    assertNull(vacancy.getDescription());
  }

  @Test
  @DisplayName("given vacancy, when description is set then getter reflects the new value")
  void givenVacancyWhenDescriptionIsSetThenGetterReflectsTheNewValue() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();
    var description =
        new VacancyDescription(
            "Summary",
            "Job description",
            "Tasks",
            "What we offer",
            "About us",
            VacancyDescriptionSource.MANUAL);

    vacancy.setDescription(description);

    assertEquals(description, vacancy.getDescription());
  }

  @Test
  @DisplayName("given vacancy, when setters are used then getters reflect the new values")
  void givenVacancyWhenSettersAreUsedThenGettersReflectTheNewValues() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();
    var location = new Location("Belgium", "Antwerp");
    var hoursPerWeek = new HoursPerWeek(BigDecimal.valueOf(20), BigDecimal.valueOf(30));

    vacancy.setJobTitle("Recruiter");
    vacancy.setCategory(VacancyCategory.HUMAN_RESOURCES);
    vacancy.setLocation(location);
    vacancy.setWorkplaceType(WorkplaceType.ONSITE);
    vacancy.setHoursPerWeek(hoursPerWeek);
    vacancy.setStatus(VacancyStatus.CLOSED);

    assertEquals("Recruiter", vacancy.getJobTitle());
    assertEquals(VacancyCategory.HUMAN_RESOURCES, vacancy.getCategory());
    assertEquals(location, vacancy.getLocation());
    assertEquals(WorkplaceType.ONSITE, vacancy.getWorkplaceType());
    assertEquals(hoursPerWeek, vacancy.getHoursPerWeek());
    assertEquals(VacancyStatus.CLOSED, vacancy.getStatus());
  }

  @Test
  @DisplayName("given two vacancies with the same id, when equals then returns true")
  void givenTwoVacanciesWithSameIdWhenEqualsThenReturnsTrue() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    assertTrue(vacancy.equals(vacancy));
  }

  @Test
  @DisplayName("given two vacancies with different ids, when equals then returns false")
  void givenTwoVacanciesWithDifferentIdsWhenEqualsThenReturnsFalse() {
    var vacancy1 = VacancyFactory.getFilledCoreVacancy().build();
    var vacancy2 = VacancyFactory.getFilledCoreVacancy().build();

    assertNotEquals(vacancy1, vacancy2);
  }

  @Test
  @DisplayName("given vacancy and non-vacancy object, when equals then returns false")
  void givenVacancyAndNonVacancyObjectWhenEqualsThenReturnsFalse() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    assertFalse(vacancy.equals("not a vacancy"));
  }

  @Test
  @DisplayName("given vacancy, when hashCode then matches hash of id")
  void givenVacancyWhenHashCodeThenMatchesHashOfId() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    assertEquals(vacancy.getId().hashCode(), vacancy.hashCode());
  }

  @Test
  @DisplayName("given vacancy, when toString then contains job title")
  void givenVacancyWhenToStringThenContainsJobTitle() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().jobTitle("Backend Engineer").build();

    assertTrue(vacancy.toString().contains("Backend Engineer"));
  }

  @Test
  @DisplayName("given vacancy with a description, when toString then description is excluded")
  void givenVacancyWithDescriptionWhenToStringThenDescriptionIsExcluded() {
    var description =
        new VacancyDescription(
            "Summary",
            "Job description",
            "Tasks",
            "What we offer",
            "About us",
            VacancyDescriptionSource.MANUAL);
    var vacancy = VacancyFactory.getFilledCoreVacancy().description(description).build();

    assertFalse(vacancy.toString().contains("Summary"));
  }
}
