package eu.talmar.api.adapter.in.rest.identity.mapper.request;

import eu.talmar.api.application.port.in.command.RegisterUserCommand;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.identity.adapter.in.web.contract.RegistrationRequest;
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
