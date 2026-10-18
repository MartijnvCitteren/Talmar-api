package eu.talmar.api.adapter.out.persistence.identity;

import eu.talmar.api.adapter.out.persistence.identity.mapper.UserJpaMapper;
import eu.talmar.api.application.port.out.UserRepository;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@code UserRepository}. Saves a user and, once confirmed, its credentials
 * in one cascaded save through the {@code @OneToOne} relation, like {@code
 * VacancyRepositoryAdapter} does for the vacancy description. The company is set as a reference (no
 * query), so the foreign key is written without loading the company.
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
    if (user.getCredentials() != null) {
      var credentialsEntity = new UserCredentialsJpaEntity();
      // Set the @MapsId-derived id explicitly: without it, Hibernate's merge cascade cannot tell an
      // update of an existing row from a new one and always attempts an INSERT.
      credentialsEntity.setId(entity.getId());
      credentialsEntity.setUser(entity);
      credentialsEntity.setPasswordHash(user.getCredentials().passwordHash());
      entity.setCredentials(credentialsEntity);
    }
    jpaRepository.save(entity);
  }

  @Override
  public Optional<User> findByEmail(EmailAddress email) {
    return jpaRepository.findByEmail(email.value()).map(jpaMapper::toDomain);
  }

  @Override
  public Optional<User> findByVerificationTokenHash(String tokenHash) {
    return jpaRepository.findByVerificationTokenHash(tokenHash).map(jpaMapper::toDomain);
  }

  @Override
  public List<User> findAwaitingVerificationExpiredAt(LocalDateTime moment) {
    return jpaRepository
        .findByStatusAndVerificationTokenExpiresAtLessThanEqual(
            UserStatus.PENDING_VERIFICATION, moment)
        .stream()
        .map(jpaMapper::toDomain)
        .toList();
  }

  @Override
  public long countByCompanyId(UUID companyId) {
    return jpaRepository.countByCompanyId(companyId);
  }

  @Override
  public void delete(User user) {
    jpaRepository.deleteById(user.getId());
  }
}
