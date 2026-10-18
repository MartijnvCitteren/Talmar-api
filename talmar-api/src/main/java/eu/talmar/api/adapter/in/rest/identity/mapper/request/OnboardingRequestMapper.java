package eu.talmar.api.adapter.in.rest.identity.mapper.request;

import eu.talmar.api.application.port.in.command.OnboardUserCommand;
import eu.talmar.api.identity.adapter.in.web.contract.OnboardingRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps the onboarding request, already validated against the contract, and the token's subject to
 * the command; the job role enums match by name.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface OnboardingRequestMapper {

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "jobRole", source = "request.jobRole")
  OnboardUserCommand toCommand(String subject, OnboardingRequest request);
}
