package app.talmar.api.adapter.in.rest.vacancy.mapper.response;

import app.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import app.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyDescriptionResponseMapper {

  VacancyDescriptionResponse toResponse(VacancyDescription description);
}
