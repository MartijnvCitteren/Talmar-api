package eu.talmar.api.adapter.in.rest.identity.mapper.request;

import eu.talmar.api.application.port.in.command.ConfirmRegistrationCommand;
import eu.talmar.api.domain.identity.valueobject.Password;
import eu.talmar.api.identity.adapter.in.web.contract.RegistrationConfirmationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps the confirmation request, whose password the contract has already checked against the
 * password policy, to its command.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RegistrationConfirmationRequestMapper {

  ConfirmRegistrationCommand toCommand(RegistrationConfirmationRequest request);

  default Password toPassword(String password) {
    return new Password(password);
  }
}
