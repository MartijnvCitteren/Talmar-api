package eu.talmar.api.application.service;

import static org.mockito.Mockito.verify;

import eu.talmar.api.application.port.out.VacancyDescriptionGenerationRepository;
import eu.talmar.api.shared.config.GenerationProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeExpiredGenerationsServiceTest {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 2, 12, 0);
  private static final Clock CLOCK = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  @Mock private VacancyDescriptionGenerationRepository generationRepository;
  @Spy private Clock clock = CLOCK;

  @Spy
  private GenerationProperties generationProperties =
      new GenerationProperties(Duration.ofHours(32));

  @InjectMocks private PurgeExpiredGenerationsService service;

  @Test
  @DisplayName(
      "given a retention period, when purge then generations requested before now minus that"
          + " period are deleted")
  void givenRetentionPeriodWhenPurgeThenDeletesBeforeNowMinusRetention() {
    service.purgeExpired();

    verify(generationRepository)
        .deleteRequestedBefore(LocalDateTime.now(clock).minus(generationProperties.retention()));
  }
}
