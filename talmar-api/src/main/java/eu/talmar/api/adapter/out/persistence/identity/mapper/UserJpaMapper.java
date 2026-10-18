package eu.talmar.api.adapter.out.persistence.identity.mapper;

import eu.talmar.api.adapter.out.persistence.identity.UserJpaEntity;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Maps between the {@code User} aggregate and {@code UserJpaEntity}. */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserJpaMapper {

  /**
   * Maps the user without its company: {@code UserRepositoryAdapter} wires it as a reference to its
   * row.
   */
  @Mapping(target = "email", source = "email.value")
  @Mapping(target = "company", ignore = true)
  UserJpaEntity toJpaEntity(User user);

  @Mapping(target = "companyId", source = "company.id")
  User toDomain(UserJpaEntity entity);

  default EmailAddress toEmailAddress(String email) {
    return email == null ? null : new EmailAddress(email);
  }
}
