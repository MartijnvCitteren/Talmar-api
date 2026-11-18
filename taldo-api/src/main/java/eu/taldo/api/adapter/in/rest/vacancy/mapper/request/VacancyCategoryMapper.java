package eu.taldo.api.adapter.in.rest.vacancy.mapper.request;

import eu.taldo.api.vacancy.adapter.in.web.contract.VacancyCategory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
interface VacancyCategoryMapper {

  eu.taldo.api.domain.vacancy.valueobject.VacancyCategory toDomainCategory(
      VacancyCategory category);
}
