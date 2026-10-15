package app.jobzy.api.adapter.in.rest.identity.mapper.response;

import app.jobzy.api.domain.identity.valueobject.UserStatus;
import app.jobzy.api.identity.adapter.in.web.contract.AccountStatus;
import app.jobzy.api.identity.adapter.in.web.contract.RegistrationConfirmationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.ValueMapping;

@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface RegistrationConfirmationResponseMapper {

  default RegistrationConfirmationResponse toResponse(UserStatus status) {
    return new RegistrationConfirmationResponse(toAccountStatus(status));
  }

  /**
   * A confirmed user is never {@code PENDING_VERIFICATION}, so the contract has no such status;
   * reaching it here is a bug, not a client error.
   */
  @ValueMapping(source = "PENDING_VERIFICATION", target = MappingConstants.THROW_EXCEPTION)
  AccountStatus toAccountStatus(UserStatus status);
}
