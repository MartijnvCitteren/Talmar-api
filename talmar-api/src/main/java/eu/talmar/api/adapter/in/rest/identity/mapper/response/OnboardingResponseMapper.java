package eu.talmar.api.adapter.in.rest.identity.mapper.response;

import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.identity.adapter.in.web.contract.AccountStatus;
import eu.talmar.api.identity.adapter.in.web.contract.OnboardingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Maps the user's status to the contract; the status enums match by name. */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OnboardingResponseMapper {

  default OnboardingResponse toResponse(UserStatus status) {
    return new OnboardingResponse(toAccountStatus(status));
  }

  AccountStatus toAccountStatus(UserStatus status);
}
