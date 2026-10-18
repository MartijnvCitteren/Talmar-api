package eu.talmar.api.application.service;

import static eu.talmar.api.testSupport.GenerationFactory.REQUESTED_AT;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationFailedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationNotFoundException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationRejectedException;
import eu.talmar.api.domain.generation.VacancyDescriptionGenerationUnavailableException;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.testSupport.GenerationFactory;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetVacancyDescriptionGenerationServiceTest {

  @Mock private VacancyDescriptionGenerationRepository generationRepository;

  private GetVacancyDescriptionGenerationService serviceAt(LocalDateTime now) {
    var clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    return new GetVacancyDescriptionGenerationService(generationRepository, clock);
  }

  @Test
  @DisplayName("given a completed generation of this vacancy, when get then it is returned")
  void givenCompletedGenerationOfThisVacancyWhenGetThenReturned() {
    var generation = GenerationFactory.completed();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));

    var result =
        serviceAt(REQUESTED_AT.plusHours(1)).get(generation.getVacancyId(), generation.getId());

    assertSame(generation, result);
  }

  @Test
  @DisplayName("given a pending generation within its deadline, when get then it is returned")
  void givenPendingGenerationWithinDeadlineWhenGetThenReturned() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));

    var result =
        serviceAt(REQUESTED_AT.plusSeconds(30)).get(generation.getVacancyId(), generation.getId());

    assertSame(generation, result);
  }

  @Test
  @DisplayName(
      "given a pending generation past its deadline, when get then it fails as a failed"
          + " generation")
  void givenPendingGenerationPastDeadlineWhenGetThenFailed() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    var service = serviceAt(REQUESTED_AT.plusSeconds(91));
    var vacancyId = generation.getVacancyId();
    var generationId = generation.getId();

    assertThrows(
        VacancyDescriptionGenerationFailedException.class,
        () -> service.get(vacancyId, generationId));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("failures")
  @DisplayName("given a failed generation, when get then it throws the exception for its failure")
  void givenFailedGenerationWhenGetThenThrowsExceptionForItsFailure(
      GenerationFailureReason reason, Class<? extends RuntimeException> expected) {
    var generation = GenerationFactory.pending();
    generation.fail(reason, REQUESTED_AT.plusSeconds(5));
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    var service = serviceAt(REQUESTED_AT.plusMinutes(5));
    var vacancyId = generation.getVacancyId();
    var generationId = generation.getId();

    assertThrows(expected, () -> service.get(vacancyId, generationId));
  }

  private static Stream<Arguments> failures() {
    return Stream.of(
        arguments(
            GenerationFailureReason.INPUT_REJECTED,
            VacancyDescriptionGenerationRejectedException.class),
        arguments(
            GenerationFailureReason.PROVIDER_UNAVAILABLE,
            VacancyDescriptionGenerationUnavailableException.class),
        arguments(
            GenerationFailureReason.PROVIDER_ERROR,
            VacancyDescriptionGenerationFailedException.class),
        arguments(
            GenerationFailureReason.TIMEOUT, VacancyDescriptionGenerationFailedException.class),
        arguments(
            GenerationFailureReason.INVALID_OUTPUT,
            VacancyDescriptionGenerationFailedException.class));
  }

  @Test
  @DisplayName("given a generation of another vacancy, when get then not found")
  void givenGenerationOfAnotherVacancyWhenGetThenNotFound() {
    var generation = GenerationFactory.pending();
    when(generationRepository.findById(generation.getId())).thenReturn(Optional.of(generation));
    var service = serviceAt(REQUESTED_AT);
    var otherVacancyId = UUID.randomUUID();
    var generationId = generation.getId();

    assertThrows(
        VacancyDescriptionGenerationNotFoundException.class,
        () -> service.get(otherVacancyId, generationId));
  }

  @Test
  @DisplayName("given an unknown generation, when get then not found")
  void givenUnknownGenerationWhenGetThenNotFound() {
    var generationId = UUID.randomUUID();
    when(generationRepository.findById(generationId)).thenReturn(Optional.empty());
    var service = serviceAt(REQUESTED_AT);
    var vacancyId = UUID.randomUUID();

    assertThrows(
        VacancyDescriptionGenerationNotFoundException.class,
        () -> service.get(vacancyId, generationId));
  }
}
