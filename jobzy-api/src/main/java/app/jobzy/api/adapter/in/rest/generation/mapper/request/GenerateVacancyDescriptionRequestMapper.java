package app.jobzy.api.adapter.in.rest.generation.mapper.request;

import app.jobzy.api.application.port.in.command.StartVacancyDescriptionGenerationCommand;
import app.jobzy.api.vacancy.adapter.in.web.contract.GenerateVacancyDescriptionRequest;
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
