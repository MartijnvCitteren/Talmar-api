package eu.talmar.api.adapter.in.rest.vacancy.mapper.response;

import eu.talmar.api.adapter.in.rest.vacancy.mapper.request.HoursPerWeekMapper;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    uses = {HoursPerWeekMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyResponseMapper {

  @Mapping(target = "minHoursPerWeek", source = "hoursPerWeek.minHours")
  @Mapping(target = "maxHoursPerWeek", source = "hoursPerWeek.maxHours")
  // Not in the domain model yet.
  @Mapping(target = "contactPerson", ignore = true)
  @Mapping(target = "offer", ignore = true)
  @Mapping(target = "publishedAt", ignore = true)
  @Mapping(target = "deletedAt", ignore = true)
  VacancyResponse toVacancyResponse(Vacancy vacancy);

  default OffsetDateTime map(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.atOffset(java.time.ZoneOffset.UTC);
  }
}
