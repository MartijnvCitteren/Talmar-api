package app.talmar.api.adapter.in.rest.vacancy.mapper.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import app.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import app.talmar.api.vacancy.adapter.in.web.contract.Language;
import app.talmar.api.vacancy.adapter.in.web.contract.Location;
import app.talmar.api.vacancy.adapter.in.web.contract.VacancyCategory;
import app.talmar.api.vacancy.adapter.in.web.contract.VacancyCoreRequest;
import app.talmar.api.vacancy.adapter.in.web.contract.WorkplaceType;
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
            .language(app.talmar.api.domain.vacancy.valueobject.Language.EN)
            .category(app.talmar.api.domain.vacancy.valueobject.VacancyCategory.ENGINEERING)
            .location(new app.talmar.api.domain.vacancy.valueobject.Location("NL", "Leiden"))
            .workplaceType(app.talmar.api.domain.vacancy.valueobject.WorkplaceType.HYBRID)
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
