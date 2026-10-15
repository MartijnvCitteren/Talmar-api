package app.jobzy.api.adapter.in.rest.vacancy.mapper.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.jobzy.api.application.port.in.command.CreateCoreVacancyCommand;
import app.jobzy.api.domain.vacancy.valueobject.HoursPerWeek;
import app.jobzy.api.vacancy.adapter.in.web.contract.Language;
import app.jobzy.api.vacancy.adapter.in.web.contract.Location;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyCategory;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyCoreRequest;
import app.jobzy.api.vacancy.adapter.in.web.contract.WorkplaceType;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(classes = VacancyCoreRequestMapperTest.MapperTestConfig.class)
class VacancyCoreRequestMapperTest {

  @Configuration
  @ComponentScan(basePackageClasses = {VacancyCoreRequestMapper.class, HoursPerWeekMapper.class})
  static class MapperTestConfig {}

  @Autowired private VacancyCoreRequestMapper mapper;

  @Test
  @DisplayName(
      "given vacancy core request, when toCommand, then maps all fields, delegating to sub-mappers")
  void givenVacancyCoreRequestWhenToDtoThenMapsAllFields() {
    var request =
        new VacancyCoreRequest(
            "Backend Engineer",
            Language.EN,
            VacancyCategory.ENGINEERING,
            new Location("NL", "Leiden"),
            WorkplaceType.HYBRID,
            BigDecimal.valueOf(32),
            BigDecimal.valueOf(40));

    var expected =
        CreateCoreVacancyCommand.builder()
            .jobTitle("Backend Engineer")
            .language(app.jobzy.api.domain.vacancy.valueobject.Language.EN)
            .category(app.jobzy.api.domain.vacancy.valueobject.VacancyCategory.ENGINEERING)
            .location(new app.jobzy.api.domain.vacancy.valueobject.Location("NL", "Leiden"))
            .workplaceType(app.jobzy.api.domain.vacancy.valueobject.WorkplaceType.HYBRID)
            .hoursPerWeek(new HoursPerWeek(BigDecimal.valueOf(32), BigDecimal.valueOf(40)))
            .build();

    CreateCoreVacancyCommand result = mapper.toCommand(request);

    assertEquals(expected.jobTitle(), result.jobTitle());
    assertEquals(expected.language(), result.language());
    assertEquals(expected.category(), result.category());
    assertEquals(expected.location(), result.location());
    assertEquals(expected.workplaceType(), result.workplaceType());
    assertEquals(expected.hoursPerWeek(), result.hoursPerWeek());
  }
}
