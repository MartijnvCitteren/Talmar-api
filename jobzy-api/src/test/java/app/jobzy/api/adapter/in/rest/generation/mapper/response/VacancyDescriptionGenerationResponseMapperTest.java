package app.jobzy.api.adapter.in.rest.generation.mapper.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.jobzy.api.testSupport.GenerationFactory;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyDescriptionGenerationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VacancyDescriptionGenerationResponseMapperTest {

  private final VacancyDescriptionGenerationResponseMapper mapper =
      new VacancyDescriptionGenerationResponseMapperImpl();

  @Test
  @DisplayName(
      "given a completed generation, when toResponse then only the generated sections are filled")
  void givenCompletedGenerationWhenToResponseThenOnlyGeneratedSectionsFilled() {
    var generation = GenerationFactory.completed();

    var response = mapper.toResponse(generation);

    assertEquals(generation.getId(), response.getGenerationId());
    assertEquals(VacancyDescriptionGenerationStatus.COMPLETED, response.getStatus());
    assertEquals(generation.getDraft().summary(), response.getDescription().getSummary());
    assertEquals(
        generation.getDraft().jobDescription(), response.getDescription().getJobDescription());
    assertEquals(generation.getDraft().tasks(), response.getDescription().getTasks());
    assertNull(response.getDescription().getWhatWeOffer());
    assertNull(response.getDescription().getAboutUs());
  }

  @Test
  @DisplayName("given a pending generation, when toResponse then there is no description")
  void givenPendingGenerationWhenToResponseThenNoDescription() {
    var response = mapper.toResponse(GenerationFactory.pending());

    assertEquals(VacancyDescriptionGenerationStatus.PENDING, response.getStatus());
    assertNull(response.getDescription());
  }
}
