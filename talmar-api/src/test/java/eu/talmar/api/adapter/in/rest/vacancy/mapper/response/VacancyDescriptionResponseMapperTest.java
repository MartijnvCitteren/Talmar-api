package eu.talmar.api.adapter.in.rest.vacancy.mapper.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyDescriptionResponseMapperTest {

  @InjectMocks private VacancyDescriptionResponseMapperImpl mapper;

  @Test
  @DisplayName("given a vacancy description, when toResponse then maps every field except source")
  void givenVacancyDescriptionWhenToResponseThenMapsEveryFieldExceptSource() {
    var description =
        new VacancyDescription(
            "Summary",
            "Job description",
            "Tasks",
            "What we offer",
            "About us",
            VacancyDescriptionSource.MANUAL);

    var result = mapper.toResponse(description);

    assertEquals(description.summary(), result.getSummary());
    assertEquals(description.jobDescription(), result.getJobDescription());
    assertEquals(description.tasks(), result.getTasks());
    assertEquals(description.whatWeOffer(), result.getWhatWeOffer());
    assertEquals(description.aboutUs(), result.getAboutUs());
  }
}
