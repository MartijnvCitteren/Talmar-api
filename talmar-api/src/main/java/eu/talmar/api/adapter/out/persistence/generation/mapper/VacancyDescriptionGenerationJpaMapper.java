package eu.talmar.api.adapter.out.persistence.generation.mapper;

import eu.talmar.api.adapter.out.persistence.generation.VacancyDescriptionGenerationJpaEntity;
import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps between {@code VacancyDescriptionGeneration} and its flattened JPA entity. The input and
 * draft value objects become prefixed columns; the audit columns are filled by JPA auditing.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyDescriptionGenerationJpaMapper {

  @Mapping(target = "jobTitle", source = "input.jobTitle")
  @Mapping(target = "language", source = "input.language")
  @Mapping(target = "mostImportantTasks", source = "input.mostImportantTasks")
  @Mapping(target = "team", source = "input.team")
  @Mapping(target = "whyNiceJob", source = "input.whyNiceJob")
  @Mapping(target = "draftSummary", source = "draft.summary")
  @Mapping(target = "draftJobDescription", source = "draft.jobDescription")
  @Mapping(target = "draftTasks", source = "draft.tasks")
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastModifiedAt", ignore = true)
  @Mapping(target = "lastModifiedBy", ignore = true)
  VacancyDescriptionGenerationJpaEntity toJpaEntity(VacancyDescriptionGeneration generation);

  @Mapping(target = "input", source = ".")
  @Mapping(target = "draft", source = ".")
  VacancyDescriptionGeneration toDomain(VacancyDescriptionGenerationJpaEntity entity);

  GenerationInput toInput(VacancyDescriptionGenerationJpaEntity entity);

  /** A row only has a draft when the generation completed; the draft columns are then all set. */
  default GeneratedDraft toDraft(VacancyDescriptionGenerationJpaEntity entity) {
    if (entity.getDraftSummary() == null) {
      return null;
    }
    return new GeneratedDraft(
        entity.getDraftSummary(), entity.getDraftJobDescription(), entity.getDraftTasks());
  }
}
