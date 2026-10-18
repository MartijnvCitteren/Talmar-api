package eu.talmar.api.adapter.out.persistence.identity;

import eu.talmar.api.adapter.out.persistence.identity.mapper.UserJpaMapper;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@code UserRepository}. The company is set as a reference (no query), so
 * the foreign key is written without loading the company.
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {
  private final UserJpaMapper jpaMapper;
  private final UserJpaRepository jpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  @Override
  public void save(User user) {
    var entity = jpaMapper.toJpaEntity(user);
    entity.setCompany(companyJpaRepository.getReferenceById(user.getCompanyId()));
    jpaRepository.save(entity);
  }

  @Override
  public Optional<User> findByEmail(EmailAddress email) {
    return jpaRepository.findByEmail(email.value()).map(jpaMapper::toDomain);
  }
}
