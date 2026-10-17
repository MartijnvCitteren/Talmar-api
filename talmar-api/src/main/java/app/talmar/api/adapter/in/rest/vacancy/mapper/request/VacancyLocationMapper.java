package app.talmar.api.adapter.in.rest.vacancy.mapper.request;

import app.talmar.api.vacancy.adapter.in.web.contract.Location;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
interface VacancyLocationMapper {
  app.talmar.api.domain.vacancy.valueobject.Location toDomainLocation(Location location);
}
