package eu.talmar.api.adapter.in.rest.identity.mapper.request;

import eu.talmar.api.application.port.in.command.ChangeNameCommand;
import eu.talmar.api.identity.adapter.in.web.contract.NameChangeRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps the name change, already validated against the contract, and the token's subject to the
 * command. An omitted name stays {@code null}: unchanged.
 */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface NameChangeRequestMapper {

  @Mapping(target = "subject", source = "subject")
  @Mapping(target = "firstName", source = "request.firstName")
  @Mapping(target = "lastName", source = "request.lastName")
  ChangeNameCommand toCommand(String subject, NameChangeRequest request);
}
