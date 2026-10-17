package app.talmar.api.adapter.out.persistence.identity;

import app.talmar.api.adapter.out.persistence.identity.mapper.CompanyJpaMapper;
import app.talmar.api.application.port.out.CompanyRepository;
import app.talmar.api.domain.identity.Company;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Persistence adapter for {@code CompanyRepository}. */
@Component
@RequiredArgsConstructor
public class CompanyRepositoryAdapter implements CompanyRepository {
  private final CompanyJpaMapper jpaMapper;
  private final CompanyJpaRepository jpaRepository;

  @Override
  public void save(Company company) {
    jpaRepository.save(jpaMapper.toJpaEntity(company));
  }

  @Override
  public Optional<Company> findByEmailDomain(String emailDomain) {
    return jpaRepository.findByEmailDomain(emailDomain).map(jpaMapper::toDomain);
  }

  @Override
  public void deleteById(UUID id) {
    jpaRepository.deleteById(id);
  }
}
