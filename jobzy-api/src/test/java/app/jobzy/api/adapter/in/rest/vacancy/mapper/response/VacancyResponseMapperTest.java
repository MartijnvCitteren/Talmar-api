package app.jobzy.api.adapter.in.rest.vacancy.mapper.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.jobzy.api.testSupport.VacancyFactory;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyResponseMapperTest {

  @InjectMocks private VacancyResponseMapperImpl mapper;

  @Test
  @DisplayName("given vacancy, when toVacancyResponse then maps all fields")
  void givenVacancyWhenToVacancyResponseThenMapsAllFields() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();

    VacancyResponse result = mapper.toVacancyResponse(vacancy);

    assertEquals(vacancy.getId(), result.getId());
    assertEquals(vacancy.getStatus().name(), result.getStatus().getValue());
    assertEquals(vacancy.getJobTitle(), result.getJobTitle());
    assertEquals(vacancy.getLanguage().name(), result.getLanguage().name());
    assertEquals(vacancy.getCategory().name(), result.getCategory().getValue());
    assertEquals(vacancy.getLocation().country(), result.getLocation().getCountry());
    assertEquals(vacancy.getLocation().city(), result.getLocation().getCity());
    assertEquals(vacancy.getWorkplaceType().name(), result.getWorkplaceType().getValue());
    assertEquals(vacancy.getHoursPerWeek().minHours(), result.getMinHoursPerWeek());
    assertEquals(vacancy.getHoursPerWeek().maxHours(), result.getMaxHoursPerWeek());
    assertEquals(vacancy.getCreatedAt().atOffset(ZoneOffset.UTC), result.getCreatedAt());
  }

  @Test
  @DisplayName("given local date time, when map then applies UTC offset")
  void givenLocalDateTimeWhenMapThenAppliesUtcOffset() {
    var localDateTime = LocalDateTime.of(2026, 3, 15, 13, 45, 30);

    OffsetDateTime result = mapper.map(localDateTime);

    assertEquals(OffsetDateTime.of(localDateTime, ZoneOffset.UTC), result);
    assertEquals(ZoneOffset.UTC, result.getOffset());
  }

  @Test
  @DisplayName("given null local date time, when map then returns null")
  void givenNullLocalDateTimeWhenMapThenReturnsNull() {
    OffsetDateTime result = mapper.map(null);

    assertNull(result);
  }
}
