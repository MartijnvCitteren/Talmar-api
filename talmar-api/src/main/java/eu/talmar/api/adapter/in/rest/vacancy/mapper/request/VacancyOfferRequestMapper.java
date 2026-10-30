package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import eu.talmar.api.application.port.in.command.SetVacancyOfferCommand;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.vacancy.valueobject.PhoneNumber;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import java.util.Currency;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyOfferRequestMapper {

  @Mapping(target = "vacancyId", source = "id")
  SetVacancyOfferCommand toCommand(UUID id, VacancyOfferRequest request);

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
