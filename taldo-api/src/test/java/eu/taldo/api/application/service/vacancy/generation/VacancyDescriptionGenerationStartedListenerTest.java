package eu.taldo.api.application.service.vacancy.generation;

import static org.mockito.Mockito.verify;

import eu.taldo.api.application.port.in.vacancy.generation.CompleteVacancyDescriptionGenerationUseCase;
import eu.taldo.api.domain.vacancy.generation.VacancyDescriptionGenerationStarted;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyDescriptionGenerationStartedListenerTest {

  @Mock private CompleteVacancyDescriptionGenerationUseCase completeUseCase;

  @InjectMocks private VacancyDescriptionGenerationStartedListener listener;

  @Test
  @DisplayName("given a started generation, when the event arrives then it is completed")
  void givenStartedGenerationWhenEventArrivesThenCompleted() {
    var generationId = UUID.randomUUID();

    listener.onGenerationStarted(new VacancyDescriptionGenerationStarted(generationId));

    verify(completeUseCase).complete(generationId);
  }
}
