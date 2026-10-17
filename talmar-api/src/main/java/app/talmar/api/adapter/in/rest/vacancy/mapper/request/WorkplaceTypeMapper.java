package app.talmar.api.adapter.in.rest.vacancy.mapper.request;

import app.talmar.api.vacancy.adapter.in.web.contract.WorkplaceType;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
interface WorkplaceTypeMapper {

  app.talmar.api.domain.vacancy.valueobject.WorkplaceType toDomainWorkplaceType(
      WorkplaceType workplaceType);
}
