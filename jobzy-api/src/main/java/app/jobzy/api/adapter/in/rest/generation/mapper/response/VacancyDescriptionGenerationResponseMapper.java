package app.jobzy.api.adapter.in.rest.generation.mapper.response;

import app.jobzy.api.domain.generation.valueobject.GeneratedDraft;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyDescriptionGeneration;
import app.jobzy.api.vacancy.adapter.in.web.contract.VacancyDescriptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps a generation to the contract's poll response. The draft only carries the generated sections;
 * {@code whatWeOffer} and {@code aboutUs} stay absent.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyDescriptionGenerationResponseMapper {

  @Mapping(target = "generationId", source = "id")
  @Mapping(target = "description", source = "draft")
  VacancyDescriptionGeneration toResponse(
      app.jobzy.api.domain.generation.VacancyDescriptionGeneration generation);

  @Mapping(target = "whatWeOffer", ignore = true)
  @Mapping(target = "aboutUs", ignore = true)
  VacancyDescriptionResponse toDescription(GeneratedDraft draft);
}
