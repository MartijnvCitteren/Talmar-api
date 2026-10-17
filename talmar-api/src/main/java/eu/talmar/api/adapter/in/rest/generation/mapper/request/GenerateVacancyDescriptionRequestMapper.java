package eu.talmar.api.adapter.in.rest.generation.mapper.request;

import eu.talmar.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import eu.talmar.api.vacancy.adapter.in.web.contract.GenerateVacancyDescriptionRequest;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface GenerateVacancyDescriptionRequestMapper {

  @Mapping(target = "vacancyId", source = "id")
  StartVacancyDescriptionGenerationCommand toCommand(
      UUID id, GenerateVacancyDescriptionRequest request);
}
