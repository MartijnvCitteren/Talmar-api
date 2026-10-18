package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyDescriptionRequestMapperTest {

  @InjectMocks private VacancyDescriptionRequestMapperImpl mapper;

  @Test
  @DisplayName("given a vacancy id and description request, when toCommand then maps all fields")
  void givenVacancyIdAndDescriptionRequestWhenToCommandThenMapsAllFields() {
    var id = UUID.randomUUID();
    var request =
        new VacancyDescriptionRequest()
            .summary("Summary")
            .jobDescription("Job description")
            .tasks("Tasks")
            .whatWeOffer("What we offer")
            .aboutUs("About us");

    var result = mapper.toCommand(id, request);

    assertEquals(id, result.vacancyId());
    assertEquals(request.getSummary(), result.summary());
    assertEquals(request.getJobDescription(), result.jobDescription());
    assertEquals(request.getTasks(), result.tasks());
    assertEquals(request.getWhatWeOffer(), result.whatWeOffer());
    assertEquals(request.getAboutUs(), result.aboutUs());
  }
}
