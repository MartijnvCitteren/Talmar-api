package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import eu.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyCoreRequest;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {
      VacancyCategoryMapper.class,
      VacancyLocationMapper.class,
      WorkplaceTypeMapper.class,
      HoursPerWeekMapper.class
    },
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyCoreRequestMapper {

  @Mapping(target = "hoursPerWeek", source = ".")
  CreateCoreVacancyCommand toCommand(VacancyCoreRequest vacancyCoreRequest);
}
