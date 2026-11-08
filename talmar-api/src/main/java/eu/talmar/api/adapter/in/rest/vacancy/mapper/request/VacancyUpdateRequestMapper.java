package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import eu.talmar.api.application.port.in.command.UpdateVacancyCommand;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.PhoneNumber;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyUpdateRequest;
import java.util.Currency;
import java.util.UUID;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps a change of a vacancy to an {@link UpdateVacancyCommand}. A section the request leaves out
 * stays {@code null} in the command, so the vacancy keeps it.
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {VacancyCategoryMapper.class, VacancyLocationMapper.class, WorkplaceTypeMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyUpdateRequestMapper {

  @Mapping(target = "vacancyId", source = "id")
  @Mapping(target = "hoursPerWeek", source = "request")
  UpdateVacancyCommand toCommand(UUID id, VacancyUpdateRequest request);

  @Mapping(target = "vacancyId", source = "id")
  @Mapping(target = "jobTitle", ignore = true)
  @Mapping(target = "language", ignore = true)
  @Mapping(target = "category", ignore = true)
  @Mapping(target = "location", ignore = true)
  @Mapping(target = "workplaceType", ignore = true)
  @Mapping(target = "hoursPerWeek", ignore = true)
  @Mapping(target = "description", ignore = true)
  UpdateVacancyCommand toCommand(UUID id, VacancyOfferRequest request);

  /** Text the user sent is a {@code MANUAL} description, also when it started as an AI draft. */
  @Mapping(target = "source", constant = "MANUAL")
  VacancyDescription toDescription(VacancyDescriptionRequest description);

  /** The validator lets minimum and maximum through only together. */
  default HoursPerWeek toHoursPerWeek(VacancyUpdateRequest request) {
    if (request.getMinHoursPerWeek() == null) {
      return null;
    }
    return new HoursPerWeek(request.getMinHoursPerWeek(), request.getMaxHoursPerWeek());
  }

  default PhoneNumber toPhoneNumber(String phone) {
    return phone == null ? null : new PhoneNumber(phone);
  }

  default EmailAddress toEmailAddress(String email) {
    return email == null ? null : new EmailAddress(email);
  }

  default Currency toCurrency(CurrencyCode currencyCode) {
    return currencyCode == null ? null : Currency.getInstance(currencyCode.getValue());
  }
}
