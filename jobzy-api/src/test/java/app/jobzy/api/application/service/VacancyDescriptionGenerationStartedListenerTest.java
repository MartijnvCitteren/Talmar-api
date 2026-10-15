package app.jobzy.api.application.service;

import static org.mockito.Mockito.verify;

import app.jobzy.api.application.port.in.CompleteVacancyDescriptionGenerationUseCase;
import app.jobzy.api.domain.generation.VacancyDescriptionGenerationStarted;
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
