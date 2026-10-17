package app.talmar.api.adapter.in.rest.vacancy.mapper.request;

import app.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import app.talmar.api.vacancy.adapter.in.web.contract.VacancyCoreRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface HoursPerWeekMapper {

  @Mapping(target = "minHours", source = "minHoursPerWeek")
  @Mapping(target = "maxHours", source = "maxHoursPerWeek")
  HoursPerWeek toHoursPerWeek(VacancyCoreRequest vacancyCoreRequest);
}
