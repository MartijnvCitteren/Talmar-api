package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.VacancyFactory.getFilledCreateCoreVacancyCommand;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import eu.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateVacancyServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-01-01T09:00:00Z"), ZoneOffset.UTC);

  @Mock private VacancyRepository vacancyRepository;

  private CreateVacancyService createVacancyService;

  @BeforeEach
  void setUp() {
    createVacancyService = new CreateVacancyService(vacancyRepository, CLOCK);
  }

  @Test
  @DisplayName("given valid command, when create core vacancy then vacancy is saved")
  void givenValidCommandWhenCreateCoreVacancyThenVacancyIsSaved() {
    CreateCoreVacancyCommand command = getFilledCreateCoreVacancyCommand().build();
    Vacancy vacancy = createVacancyService.createCoreVacancy(command);
    verify(vacancyRepository).save(vacancy);
  }

  @Test
  @DisplayName("given valid command, when create core vacancy then createdAt is the clock's time")
  void givenValidCommandWhenCreateCoreVacancyThenCreatedAtIsTheClocksTime() {
    CreateCoreVacancyCommand command = getFilledCreateCoreVacancyCommand().build();

    Vacancy vacancy = createVacancyService.createCoreVacancy(command);

    assertEquals(LocalDateTime.of(2026, 1, 1, 9, 0), vacancy.getCreatedAt());
  }
}
