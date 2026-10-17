package app.talmar.api.adapter.in.rest.vacancy.mapper.request;

import app.talmar.api.application.port.in.command.SetManualVacancyDescriptionCommand;
import app.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyDescriptionRequestMapper {

  @Mapping(target = "vacancyId", source = "id")
  SetManualVacancyDescriptionCommand toCommand(UUID id, VacancyDescriptionRequest request);
}
