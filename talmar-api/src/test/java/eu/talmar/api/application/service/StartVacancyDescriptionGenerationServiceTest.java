package eu.talmar.api.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationStarted;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import eu.talmar.api.testSupport.VacancyFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class StartVacancyDescriptionGenerationServiceTest {

  private static final Clock CLOCK =
      Clock.fixed(LocalDateTime.of(2026, 3, 1, 10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  @Mock private VacancyRepository vacancyRepository;
  @Mock private VacancyDescriptionGenerationRepository generationRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Spy private Clock clock = CLOCK;

  @InjectMocks private StartVacancyDescriptionGenerationService service;

  @Test
  @DisplayName(
      "given an existing vacancy, when start then a generation requested now is saved and"
          + " announced")
  void givenExistingVacancyWhenStartThenGenerationRequestedNowSavedAndAnnounced() {
    var vacancy = VacancyFactory.getFilledCoreVacancy().build();
    when(vacancyRepository.findById(vacancy.getId())).thenReturn(Optional.of(vacancy));

    var pendingGeneration = service.start(commandFor(vacancy.getId()));

    assertEquals(LocalDateTime.now(clock), pendingGeneration.getRequestedAt());
    verify(generationRepository).save(pendingGeneration);
    verify(eventPublisher)
        .publishEvent(new VacancyDescriptionGenerationStarted(pendingGeneration.getId()));
  }

  @Test
  @DisplayName("given an unknown vacancy, when start then throws and nothing is saved or announced")
  void givenUnknownVacancyWhenStartThenThrowsAndNothingSaved() {
    var vacancyId = UUID.randomUUID();
    when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.empty());
    var command = commandFor(vacancyId);

    assertThrows(VacancyNotFoundException.class, () -> service.start(command));

    verify(generationRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  private static StartVacancyDescriptionGenerationCommand commandFor(UUID vacancyId) {
    return StartVacancyDescriptionGenerationCommand.builder()
        .vacancyId(vacancyId)
        .mostImportantTasks("Tasks")
        .team("Team")
        .whyNiceJob("Why")
        .build();
  }
}
