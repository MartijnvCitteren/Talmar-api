package eu.talmar.api.adapter.out.persistence.vacancy.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyDescriptionJpaEntity;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyDescriptionJpaMapperTest {

  @InjectMocks private VacancyDescriptionJpaMapperImpl mapper;

  @Test
  @DisplayName("given entity, when toDomain then maps all fields")
  void givenEntityWhenToDomainThenMapsAllFields() {
    var entity = new VacancyDescriptionJpaEntity();
    entity.setSummary("Summary");
    entity.setJobDescription("Job description");
    entity.setTasks("Tasks");
    entity.setWhatWeOffer("What we offer");
    entity.setAboutUs("About us");
    entity.setSource(VacancyDescriptionSource.MANUAL);

    VacancyDescription result = mapper.toDomain(entity);

    assertEquals(entity.getSummary(), result.summary());
    assertEquals(entity.getJobDescription(), result.jobDescription());
    assertEquals(entity.getTasks(), result.tasks());
    assertEquals(entity.getWhatWeOffer(), result.whatWeOffer());
    assertEquals(entity.getAboutUs(), result.aboutUs());
    assertEquals(entity.getSource(), result.source());
  }

  @Test
  @DisplayName("given description VO, when toJpaEntity then maps the five text fields and source")
  void givenDescriptionVoWhenToJpaEntityThenMapsTextFieldsAndSource() {
    var description =
        new VacancyDescription(
            "Summary",
            "Job description",
            "Tasks",
            "What we offer",
            "About us",
            VacancyDescriptionSource.MANUAL);

    var result = mapper.toJpaEntity(description);

    assertEquals(description.summary(), result.getSummary());
    assertEquals(description.jobDescription(), result.getJobDescription());
    assertEquals(description.tasks(), result.getTasks());
    assertEquals(description.whatWeOffer(), result.getWhatWeOffer());
    assertEquals(description.aboutUs(), result.getAboutUs());
    assertEquals(description.source(), result.getSource());
  }
}
