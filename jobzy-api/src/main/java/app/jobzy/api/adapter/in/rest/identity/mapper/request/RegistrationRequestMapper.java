package app.jobzy.api.adapter.in.rest.identity.mapper.request;

import app.jobzy.api.application.port.in.command.RegisterUserCommand;
import app.jobzy.api.domain.identity.valueobject.EmailAddress;
import app.jobzy.api.identity.adapter.in.web.contract.RegistrationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Maps the registration request, already validated against the contract, to its command; the job
 * role enums match by name.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RegistrationRequestMapper {

  RegisterUserCommand toCommand(RegistrationRequest request);

  default EmailAddress toEmailAddress(String email) {
    return new EmailAddress(email);
  }
}
