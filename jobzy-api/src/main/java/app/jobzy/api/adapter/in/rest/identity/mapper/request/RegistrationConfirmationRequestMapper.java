package app.jobzy.api.adapter.in.rest.identity.mapper.request;

import app.jobzy.api.application.port.in.command.ConfirmRegistrationCommand;
import app.jobzy.api.domain.identity.valueobject.Password;
import app.jobzy.api.identity.adapter.in.web.contract.RegistrationConfirmationRequest;
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
